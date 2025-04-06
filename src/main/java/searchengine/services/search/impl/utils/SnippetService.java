package searchengine.services.search.impl.utils;

import org.springframework.stereotype.Service;
import searchengine.services.indexing.impl.morphology.Lemmatizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SnippetService {
    private final Lemmatizer<String> lemmatizer = new Lemmatizer<>(text -> text);
    private final static int GROUP_MAX_DISTANCE = 30;
    private final static int SNIPPET_LENGTH = 200;

    public String generateSnippet(String content, List<String> queryLemmas) {
        List<String> query = lemmatizer.findWordsWithQueryLemmas(content, queryLemmas);
        String correctedContent = getCorrectedContent(content, query);

        Pattern boldPattern = Pattern.compile("<b>(.*?)</b>");
        Matcher matcher = boldPattern.matcher(correctedContent);

        List<Integer> boldStartIndices = new ArrayList<>();
        List<Integer> boldEndIndices = new ArrayList<>();

        while(matcher.find()){
            boldStartIndices.add(matcher.start());
            boldEndIndices.add(matcher.end());
        }

        return getSnippet(boldStartIndices, boldEndIndices,
                content, correctedContent);
    }

    private String getSnippet(List<Integer> boldStartIndices, List<Integer> boldEndIndices,
                              String content, String correctedContent) {
        int index;
        int groupStartIndex = -1;

        for (int i = 0; i < boldStartIndices.size() - 1; i++){
            if(boldStartIndices.get(i+1) - boldEndIndices.get(i) <= GROUP_MAX_DISTANCE){
                groupStartIndex = boldStartIndices.get(i);
                break;
            }
        }

        if(groupStartIndex != -1) {
            index = groupStartIndex;
        } else if(!boldStartIndices.isEmpty()){
            index = boldStartIndices.get(0);
        } else {
            return content.substring(0, Math.min(SNIPPET_LENGTH, content.length())) + "...";
        }

        int start = Math.max(0, index);
        int end = Math.min(correctedContent.length(), start + SNIPPET_LENGTH);
        String snippet = correctedContent.substring(start, end);

        return snippet + "...";
    }

    private String getCorrectedContent(String content, List<String> query) {
        String correctedContent = content;
        for (String word : query) {
            String pattern = "\\b" + Pattern.quote(word) + "\\b";
            correctedContent = Pattern.compile(pattern)
                    .matcher(correctedContent)
                    .replaceAll("<b>" + word + "</b>");
        }
        return correctedContent;
    }
}
