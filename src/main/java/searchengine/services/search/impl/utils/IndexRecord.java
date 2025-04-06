package searchengine.services.search.impl.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndexRecord {
    private long pageId;
    private long lemmaId;
    private double rank;
}
