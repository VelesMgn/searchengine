package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotalStatistics {
    private int sites;
    private long pages;
    private long lemmas;
    private boolean isIndexing;
}
