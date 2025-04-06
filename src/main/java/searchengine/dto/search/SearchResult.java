package searchengine.dto.search;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResult {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
}