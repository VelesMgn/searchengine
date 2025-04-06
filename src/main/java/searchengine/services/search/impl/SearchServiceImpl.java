package searchengine.services.search.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResult;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.impl.morphology.Lemmatizer;
import searchengine.services.indexing.impl.persistence.utils.UrlUtils;
import searchengine.services.search.SearchService;
import searchengine.services.search.impl.utils.LemmaFrequency;
import searchengine.services.search.impl.utils.SnippetService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final static double THRESHOLD_PERCENTAGE = 0.8;
    private final static int DEFAULT_OFFSET = 0;
    private final static int DEFAULT_LIMIT = 20;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SnippetService snippetService;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if(query == null || query.trim().isEmpty())  {
            return new SearchResponse(false, "An empty search query is set.");
        }

        offset = (offset == null || offset < 0) ? DEFAULT_OFFSET : offset;
        limit = (limit == null || limit < 1) ? DEFAULT_LIMIT : limit;

        Site siteFromRequest = getSite(site);
        if(site != null && siteFromRequest == null) {
            return new SearchResponse(false, "The specified site is not indexed.");
        }

        List<String> queryLemmasList = extractLemmas(query);
        if(queryLemmasList.isEmpty()) {
            return new SearchResponse(false, "The search query does not contain valid words");
        }

        int totalPages = getTotalPage(siteFromRequest);

        List<LemmaFrequency> lemmaFrequencyList = getLemmaFrequencyList(queryLemmasList, siteFromRequest, totalPages);
        if(lemmaFrequencyList.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>());
        }

        lemmaFrequencyList.sort(Comparator.comparingInt(LemmaFrequency::getFrequency));
        Set<Long> pageIds = getPageIds(lemmaFrequencyList, siteFromRequest);

        if(pageIds.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>());
        }

        Map<Long, Double> pageAbsRelevanceMap = getPageAbsRelevanceMap(pageIds, queryLemmasList, siteFromRequest);
        double maxAbsRelevance = Collections.max(pageAbsRelevanceMap.values());

        List<SearchResult> results = getSearchResultsList(pageAbsRelevanceMap, maxAbsRelevance, queryLemmasList);
        results.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));

        int totalCount = results.size();
        int fromIndex = Math.min(offset, totalCount);
        int toIndex = Math.min(offset + limit, totalCount);

        List<SearchResult> paginatedResults = results.subList(fromIndex, toIndex);

        return new SearchResponse(true, totalCount, paginatedResults);
    }

    private List<SearchResult> getSearchResultsList(Map<Long, Double> pageAbsRelevanceMap,
                                                    double maxAbsRelevance, List<String> queryLemmasList) {
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : pageAbsRelevanceMap.entrySet()) {
            long pageId = entry.getKey();
            double absRelevance = entry.getValue();
            double relRelevance = absRelevance / maxAbsRelevance;
            SearchResult searchResult = buildSearchResult(pageId, relRelevance, queryLemmasList);

            results.add(searchResult);
        }

        return results;
    }

    private SearchResult buildSearchResult(long pageId, double relRelevance, List<String> queryLemmasList) {
        Page page = pageRepository.getPagesById(pageId);
        Site siteFromDb = siteRepository.getSiteById(page.getSiteId().getId());
        Document document = getDocument(page);
        String title = document.title();
        String snippet = snippetService.generateSnippet(document.text(), queryLemmasList);

        return SearchResult.builder()
                .site(siteFromDb.getUrl())
                .siteName(siteFromDb.getName())
                .uri(page.getPath())
                .title(title)
                .snippet(snippet)
                .relevance(relRelevance)
                .build();
    }

    private List<LemmaFrequency> getLemmaFrequencyList(List<String> lemmas, Site site, int totalPages) {
        List<LemmaFrequency> lemmaFrequencyList = new ArrayList<>();

        for (String lemma : lemmas) {
            int frequency = (site != null)
                    ? lemmaRepository.getFrequencyOnSite(lemma, site.getId())
                    : lemmaRepository.getFrequency(lemma);

            if(frequency == 0) continue;

            double ratio = (double) frequency / totalPages;

            if(ratio > THRESHOLD_PERCENTAGE) continue;

            lemmaFrequencyList.add(new LemmaFrequency(lemma, frequency));
        }

        return lemmaFrequencyList;
    }

    private Map<Long, Double> getPageAbsRelevanceMap(Set<Long> pageIds, List<String> queryLemmasList,
                                                     Site siteFromRequest) {
        Map<Long, Double> pageAbsRelevanceMap = new HashMap<>();
        for (Long pageId : pageIds) {
            List<Double> ranksList = getRanksList(pageId, queryLemmasList, siteFromRequest);
            double absRelevance = 0;

            for (double rank : ranksList) {
                absRelevance += rank;
            }
            pageAbsRelevanceMap.put(pageId, absRelevance);
        }

        return pageAbsRelevanceMap;
    }

    private List<Double> getRanksList(long pageId, List<String> lemmas, Site siteFromRequest) {
        List<Double> ranksList = new ArrayList<>();

        for (String lemma : lemmas) {
            long lemmaId;
            Float rank;
            Set<Long> lemmaIds;

            if(siteFromRequest != null) {
                lemmaId = lemmaRepository.findIdByLemmaAndSiteId(lemma, siteFromRequest.getId());
                rank = indexRepository.getRankByLemmaIdAndPageId(pageId, lemmaId);
            } else {
                lemmaIds = lemmaRepository.findIdsByLemma(lemma);
                rank = indexRepository.getTotalRankByPageIdAndLemmaIds(pageId, lemmaIds);
            }

            if(rank != null) {
                ranksList.add(rank.doubleValue());
            }
        }

        return ranksList;
    }

    private Set<Long> getPageIds(List<LemmaFrequency> lemmaFrequencyList, Site siteFromRequest) {
        Set<Long> pageIds = null;

        for (LemmaFrequency lemmaFreq : lemmaFrequencyList) {
            Set<Long> currentPageIds;

            if(siteFromRequest != null) {
                long lemmaId = lemmaRepository.findIdByLemmaAndSiteId(lemmaFreq.getLemma(), siteFromRequest.getId());
                currentPageIds = indexRepository.getPageIdsByLemmaId(lemmaId);
            } else {
                Set<Long> lemmaIds = lemmaRepository.findAllIdsByLemma(lemmaFreq.getLemma());
                currentPageIds = indexRepository.getPageIdsByLemmaIds(lemmaIds);
            }

            if(currentPageIds.isEmpty()) return new HashSet<>();
            if(pageIds == null) {
                pageIds = new HashSet<>(currentPageIds);
            } else {
                pageIds.retainAll(currentPageIds);
                if(pageIds.isEmpty()) return new HashSet<>();
            }
        }

        return pageIds;
    }

    private int getTotalPage(Site site) {
        return (site != null)
                ? pageRepository.countPageBySiteId(site)
                : pageRepository.countAllPage();
    }

    private List<String> extractLemmas(String query) {
        Lemmatizer<String> lemmatizer = new Lemmatizer<>(text -> text);
        return new ArrayList<>(lemmatizer.createLemmaMap(query).keySet());
    }

    private Site getSite(String siteUrl) {
        if(siteUrl == null || siteUrl.isBlank()) return null;
        String normalizedUrl = UrlUtils.normalizeSiteUrl(siteUrl);
        Site site = siteRepository.findSiteByUrl(normalizedUrl);

        return (site != null && Status.INDEXED.equals(site.getStatus())) ? site : null;
    }

    private Document getDocument(Page page) {
        Document document = Jsoup.parse(page.getContent());
        document.select("script, style, code, pre, noscript, " +
                "iframe, object, embed, link, meta, nav, footer, " +
                "aside, form, input, button, select, textarea, " +
                "label, canvas, svg, figure, figcaption, picture, " +
                "source").remove();
        document.select("[style~=(?i)display\\s*:\\s*none]").remove();
        return document;
    }
}