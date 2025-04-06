package searchengine.services.indexing.impl.persistence.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.services.indexing.impl.persistence.batch.BatchInserter;
import searchengine.services.indexing.impl.persistence.batch.setters.IndexStatementSetter;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public final class IndexService {
    private static final List<Index> INDEX_BUFFER = Collections.synchronizedList(new ArrayList<>());
    private static final int BATCH_SIZE_FOR_INDEX = 1000;

    private final IndexRepository indexRepository;
    private final DataSource dataSource;

    public void save(Page page, Lemma lemma, float count) {
        indexRepository.insertIndex(page, lemma, count);
    }

    public void updateLemmaIdInIndex(Map<Lemma, Lemma> oldToNewLemmaMap) {
        for (Index index : INDEX_BUFFER) {
            Lemma oldLemma = index.getLemmaId();
            Lemma newLemma = oldToNewLemmaMap.get(oldLemma);
            if (newLemma != null && oldLemma != newLemma) {
                index.setLemmaId(newLemma);
            }
        }
    }

    public void createBatchInsertForIndex(AtomicBoolean stopRequested) {
        log.info("Create batch insert for index");
        String indexSql = "INSERT INTO `index` (page_id, lemma_id,`rank`) VALUES (?, ?, ?)";
        BatchInserter inserter = new BatchInserter();
        IndexStatementSetter setter = new IndexStatementSetter();

        inserter.batchInsert(
                INDEX_BUFFER,
                indexSql,
                setter,
                dataSource,
                BATCH_SIZE_FOR_INDEX,
                stopRequested
        );
    }

    public void addIndex(Page page, Lemma lemma, float count) {
        INDEX_BUFFER.add(Index.builder()
                .pageId(page)
                .lemmaId(lemma)
                .rank(count)
                .build());
    }

    public void deleteIndex(Lemma lemma) {
        indexRepository.deleteByLemmaId(lemma);
    }

    public void deleteAll() {
        indexRepository.deleteAll();
    }

    public void clearBuffer() {
        INDEX_BUFFER.clear();
    }
}