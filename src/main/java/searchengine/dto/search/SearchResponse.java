package searchengine.dto.search;

import lombok.Getter;

import java.util.List;

@Getter
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchResult> data;
    private String error;

    public SearchResponse(boolean result, int count, List<SearchResult> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
