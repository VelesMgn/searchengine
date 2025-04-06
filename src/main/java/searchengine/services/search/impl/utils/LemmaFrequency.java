package searchengine.services.search.impl.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LemmaFrequency {
    private String lemma;
    private int frequency;
}
