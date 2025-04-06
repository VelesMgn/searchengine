package searchengine.services.indexing.impl.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.enums.Status;
import searchengine.services.indexing.impl.persistence.IndexingCoordinator;
import searchengine.services.indexing.impl.morphology.Lemmatizer;
import searchengine.services.indexing.impl.parser.node.Node;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public final class PageCrawlerTask extends RecursiveAction {
    private static final Pattern FORBIDDEN_PATTERN =
            Pattern.compile("(?i).+\\.(jpg|jpeg|png|gif|bmp|pdf)(\\?.*)?$");
    private static final AtomicInteger PAGE_COUNTER = new AtomicInteger(0);
    private static final Set<String> ALL_LINKS = ConcurrentHashMap.newKeySet();
    private static final int TIME_POLITE_DELAY = 100;
    private static final int HTTP_ERROR_CODE = 400;
    private static final int FREQUENCY = 1;

    private final IndexingCoordinator coordinator;
    private final AtomicBoolean stopRequested;
    private final Node parent;

    @Override
    protected void compute() {
        ALL_LINKS.add(parent.getParentUrl());

        try {
            politeDelay();
            if (stopRequested.get()) return;

            Document document = getConnect(parent.getParentUrl());
            savePageInDb(document);

            List<PageCrawlerTask> tasks = document.select("a[href^=/]").stream()
                    .map(this::createTask).filter(Objects::nonNull).toList();

            invokeAll(tasks);
        } catch (IOException e) {
            log.error("Error in connection: {}", String.valueOf(e));
            coordinator.saveFinalSiteData(parent.getSite().getId(), Status.FAILED, String.valueOf(e));
        }
    }

    private void politeDelay() {
        try {
            Thread.sleep(TIME_POLITE_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while processing URL: {}", parent.getParentUrl(), e);
        }
    }

    private PageCrawlerTask createTask(Element element) {
        String childUrl = element.absUrl("href");
        if(shouldSkipUrl(childUrl)) return null;

        Node child = new Node(parent.getConfig(), parent.getSite(), childUrl);
        parent.addChildren(child);

        return new PageCrawlerTask(coordinator, stopRequested, child);
    }

    private void savePageInDb(Document document) {
        String siteUrl = parent.getSite().getUrl();
        String pageUrl = parent.getParentUrl();
        int statusCode = document.connection().response().statusCode();

        if (pageUrl.equals(siteUrl) || statusCode >= HTTP_ERROR_CODE) return;

        log.info("{} {}", PAGE_COUNTER.incrementAndGet(), parent.getParentUrl());
        Page page = coordinator.createPageBatchInsert(parent.getSite(), pageUrl, statusCode, document);

        createLemmaAndIndex(document, page);
    }

    private void createLemmaAndIndex(Document document, Page page) {
        Lemmatizer<Document> lemmatizer = new Lemmatizer<>(Element::text);
        Map<String, Integer> lemmaList = lemmatizer.createLemmaMap(document);

        lemmaList.forEach((lemmaText, rank) -> {
            Lemma lemma = coordinator.getLemma(parent.getSite(), lemmaText, FREQUENCY);
            coordinator.createIndex(page, lemma, rank);
        });
    }

    private Document getConnect(String parentUrl) throws IOException {
        return Jsoup.connect(parentUrl)
                .userAgent(parent.getConfig().getUserAgent())
                .referrer(parent.getConfig().getReferer())
                .ignoreHttpErrors(true)
                .timeout(15000)
                .get();
    }

    private boolean shouldSkipUrl(String childLink) {
        return !childLink.startsWith(parent.getSite().getUrl())
                || childLink.contains("#")
                || FORBIDDEN_PATTERN.matcher(childLink).matches()
                || !ALL_LINKS.add(childLink)
                || stopRequested.get();
    }

    public static void clearAllLinks() {
        PAGE_COUNTER.set(0);
        ALL_LINKS.clear();
    }
}