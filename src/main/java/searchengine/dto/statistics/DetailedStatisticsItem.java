package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DetailedStatisticsItem {
    private String name;
    private String url;
    private String status;
    private LocalDateTime statusTime;
    private String error;
    private long pages;
    private long lemmas;
}
