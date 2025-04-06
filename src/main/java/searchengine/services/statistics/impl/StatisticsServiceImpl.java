package searchengine.services.statistics.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.statistics.StatisticsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        List<Site> allSites = siteRepository.findAll();

        long totalPages = 0;
        long totalLemmas = 0;
        boolean isIndexing = false;

        List<DetailedStatisticsItem> detailedStats = new ArrayList<>();

        for (Site site : allSites) {
            long pageCount = pageRepository.countBySite(site.getId());
            long lemmaCount = lemmaRepository.countBy(site.getId());

            totalPages += pageCount;
            totalLemmas += lemmaCount;

            if (site.getStatus() == Status.INDEXING) {
                isIndexing = true;
            }

            DetailedStatisticsItem item = DetailedStatisticsItem.builder()
                    .name(site.getName())
                    .url(site.getUrl())
                    .status(site.getStatus().name())
                    .statusTime(site.getStatusTime())
                    .error(Optional.ofNullable(site.getLastError()).orElse(""))
                    .pages(pageCount)
                    .lemmas(lemmaCount)
                    .build();

            detailedStats.add(item);
        }

        TotalStatistics total = TotalStatistics.builder()
                .sites(allSites.size())
                .pages(totalPages)
                .lemmas(totalLemmas)
                .isIndexing(isIndexing)
                .build();

        StatisticsData statisticsData = StatisticsData.builder()
                .total(total)
                .detailed(detailedStats)
                .build();

        return StatisticsResponse.builder()
                .result(true)
                .statistics(statisticsData)
                .build();
    }
}
