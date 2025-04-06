package searchengine.services.indexing.impl.persistence;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SiteFromList;
import searchengine.config.SitesListConfig;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.services.indexing.impl.persistence.batch.PageBatchInserter;
import searchengine.services.indexing.impl.persistence.utils.IndexService;
import searchengine.services.indexing.impl.persistence.utils.LemmaService;
import searchengine.services.indexing.impl.persistence.utils.PageService;
import searchengine.services.indexing.impl.persistence.utils.SiteService;
import searchengine.services.indexing.impl.persistence.utils.UrlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public final class IndexingCoordinator {
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final PageBatchInserter pageBatchInserter;

    public void clearAllBuffers() {
        indexService.clearBuffer();
        lemmaService.clearBuffer();
        pageBatchInserter.clearBuffer();
    }

    public void deleteDataAboutSites() {
        indexService.deleteAll();
        lemmaService.deleteAll();
        pageService.deleteAll();
        siteService.deleteAll();
    }

    public Site getSiteFromListAndSave(SiteFromList siteFromList) {
        return siteService.save(siteFromList);
    }

    public void saveFinalSiteData(long id, Status status, String error) {
        siteService.updateStatus(id, status, error);
    }

    public Page savePage(Site site, String path, int statusCode, Document document) {
        return pageService.save(site, path, statusCode, document);
    }

    public Lemma saveLemma(Site site, String lemmaText) {
        return lemmaService.saveOrUpdate(site, lemmaText);
    }

    public void saveIndex(Page page, Lemma lemma, Integer count) {
        indexService.save(page, lemma, count);
    }

    public Site getSite(String path, SitesListConfig sitesFromConfigFile) {
        return siteService.getSiteForOnePage(path, sitesFromConfigFile);
    }

    public Site getSiteById(long id) {
        return siteService.findById(id);
    }

    public Lemma getLemma(Site site, String lemmaText, int frequency) {
        return lemmaService.addLemma(site, lemmaText, frequency);
    }

    public Page createPageBatchInsert(Site site, String pageUrl, int statusCode, Document document) {
        Page page = pageService.buildPage(site, pageUrl, statusCode, document);
        pageBatchInserter.addPage(page);

        return page;
    }

    public void createIndex(Page page, Lemma lemma, Integer rank) {
        indexService.addIndex(page, lemma, rank);
    }

    public void deletePageData(String path, Site site, Map<String, Integer> lemmaList) {
        String editedPath = UrlUtils.extractPath(path, site.getUrl());
        Page page = pageService.getPage(editedPath);

        if (page != null) {
            deleteLemmaAndIndexForOnePage(lemmaList);
            pageService.deleteById(page.getId());
        }
    }

    private void deleteLemmaAndIndexForOnePage(Map<String, Integer> lemmaList) {
        if (lemmaList.isEmpty()) return;

        List<Lemma> lemmas = lemmaService.findLemmas(lemmaList.keySet());
        List<Lemma> lemmasToUpdate = new ArrayList<>();
        List<Lemma> lemmasToDelete = new ArrayList<>();

        lemmas.forEach( lemma -> {
            indexService.deleteIndex(lemma);

            if (lemma.getFrequency() > 1) {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmasToUpdate.add(lemma);
            } else {
                lemmasToDelete.add(lemma);
            }
        });

        if (!lemmasToUpdate.isEmpty()) lemmaService.saveAll(lemmasToUpdate);
        if (!lemmasToDelete.isEmpty()) lemmaService.deleteAllFromList(lemmasToDelete);
    }

    public void addLemmaAndIndexToTheDatabase(AtomicBoolean stopRequested) {
        pageBatchInserter.flushRemainingPages();
        Map<Lemma, Lemma> oldToNewLemmaMap = lemmaService.updatingTheDuplicateKey();
        indexService.updateLemmaIdInIndex(oldToNewLemmaMap);
        lemmaService.createBatchInsertForLemma(stopRequested);
        indexService.createBatchInsertForIndex(stopRequested);
    }
}