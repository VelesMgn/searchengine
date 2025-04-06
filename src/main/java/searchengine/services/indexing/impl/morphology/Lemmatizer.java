package searchengine.services.indexing.impl.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class Lemmatizer<T> {
    private static final Set<String> SERVICE_PARTS_OF_SPEECH = Set.of("МЕЖД", "СОЮЗ", "ПРЕДЛ", "ЧАСТ");
    private static final Pattern WORD_PATTERN = Pattern.compile("[а-яА-ЯёЁ]+");
    private final Function<T, String> textExtractor;
    private final LuceneMorphology morphology;


    public Lemmatizer(Function<T, String> textExtractor) {
        this.textExtractor = textExtractor;

        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error("Error creating an instance of the lemmatizer class: {}", String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> createLemmaMap(T input) {
        String text = normalizeText(textExtractor.apply(input));
        return WORD_PATTERN.matcher(text).results()
                .map(match -> match.group().toLowerCase())
                .map(word -> morphology.getNormalForms(word).get(0))
                .filter(word -> !isServicePartOfSpeech(word))
                .filter(word -> word.length() >= 3)
                .collect(Collectors.toMap(lemma -> lemma, lemma -> 1, Integer::sum));
    }

    public List<String> findWordsWithQueryLemmas(String content, List<String> queryLemmas) {
        content = normalizeText(content);
        Map<String, String> collect = WORD_PATTERN.matcher(content).results()
                .map(MatchResult::group)
                .map(word -> {
                    String lemma = morphology.getNormalForms(word.toLowerCase()).get(0);
                    return Map.entry(word, lemma);
                })
                .filter(entry -> !isServicePartOfSpeech(entry.getValue()))
                .filter(entry -> entry.getValue().length() >= 3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1
                ));

        collect.entrySet().removeIf(word -> !queryLemmas.contains(word.getValue()));

        return new ArrayList<>(collect.keySet());
    }

    private boolean isServicePartOfSpeech(String word) {
        return morphology.getMorphInfo(word).stream()
                .anyMatch(info -> SERVICE_PARTS_OF_SPEECH.stream().anyMatch(info::contains));
    }

    private String normalizeText(String text) {
        return text.replace('ё', 'е').replace('Ё', 'Е');
    }
}