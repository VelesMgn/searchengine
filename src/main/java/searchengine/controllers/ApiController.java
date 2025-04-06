package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        log.info("The controller \"statistics\" calls the statistics service.");
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        log.info("The controller \"startIndexing\" calls the startIndexing service.");
        boolean isIndexed = indexingService.startIndexing();

        return ResponseEntity.ok(isIndexed
                ? Map.of("result", true)
                : Map.of("result", false,
                "error", "Indexing has already started.")
        );
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        log.info("The controller \"stopIndexing\" calls the stopIndexing service.");
        boolean isIndexed = indexingService.stopIndexing();

        return ResponseEntity.ok(isIndexed
                ? Map.of("result", true)
                : Map.of("result", false,
                "error", "Indexing is not running.")
        );
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        log.info("The controller \"indexPage\" calls the indexPage service.");
        boolean isIndexed = indexingService.indexPage(url);

        return ResponseEntity.ok(isIndexed
                ? Map.of("result", true)
                : Map.of("result", false,
                "error", "This page is located outside the sites specified in the configuration file " +
                        "or the site are begin indexed.")
        );
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(value="query", required=false) String query,
                                    @RequestParam(value="site", required=false) String site,
                                    @RequestParam(value="offset", required=false) Integer offset,
                                    @RequestParam(value="limit", required=false) Integer limit) {
        log.info("The controller \"search\" calls the search service.");
        SearchResponse response = searchService.search(query, site, offset, limit);

        if(!response.isResult()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
