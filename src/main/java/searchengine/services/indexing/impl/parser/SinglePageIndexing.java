package searchengine.services.indexing.impl.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.services.indexing.impl.persistence.IndexingCoordinator;
import searchengine.services.indexing.impl.morphology.Lemmatizer;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public final class SinglePageIndexing {
    private final IndexingCoordinator coordinator;
    private final Site site;

    public boolean getIndexPageResult(String path) {
        try {
            Document document = Jsoup.connect(path).timeout(15000).get();
            int statusCode = document.connection().response().statusCode();
            updateDataInDb(path, document, statusCode);

            log.info("The page at: {} has been updated", path);
            return true;

        } catch (IOException e) {
            String errorText = "Error connecting to the page: " + path;
            coordinator.saveFinalSiteData(site.getId(), Status.FAILED, errorText);
            log.error(errorText);
            return false;
        }
    }

    private void updateDataInDb(String path, Document document, int statusCode) {
        Lemmatizer<Document> lemmatizer = new Lemmatizer<>(doc -> doc.body().text());
        Map<String, Integer> lemmaList = lemmatizer.createLemmaMap(document);

        coordinator.deletePageData(path, site, lemmaList);
        Page page = coordinator.savePage(site, path, statusCode, document);

        lemmaList.forEach((lemmaText, count) -> {
            Lemma lemma = coordinator.saveLemma(site, lemmaText);
            coordinator.saveIndex(page, lemma, count);
        });

        coordinator.saveFinalSiteData(site.getId(), Status.INDEXED, "");
    }
}
