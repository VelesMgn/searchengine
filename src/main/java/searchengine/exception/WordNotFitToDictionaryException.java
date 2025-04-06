package searchengine.exception;

public class WordNotFitToDictionaryException extends RuntimeException {
    public WordNotFitToDictionaryException(String word) {
        super("Words <" + word + "> that are not in the dictionary!");
    }
}
