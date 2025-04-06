package searchengine.services.indexing;

public interface IndexingService {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String path);
}
