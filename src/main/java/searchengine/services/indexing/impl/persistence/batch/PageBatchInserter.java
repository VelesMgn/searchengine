package searchengine.services.indexing.impl.persistence.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.services.indexing.impl.persistence.batch.setters.PageStatementSetter;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public final class PageBatchInserter {
    private static final List<Page> PAGE_BUFFER = Collections.synchronizedList(new ArrayList<>());
    private static final int PAGE_BATCH_SIZE = 100;

    private final DataSource dataSource;

    public void addPage(Page page) {
        PAGE_BUFFER.add(page);
        if (PAGE_BUFFER.size() >= PAGE_BATCH_SIZE) flush();
    }

    private void flush() {
        if (PAGE_BUFFER.isEmpty()) return;

        List<Page> batchToInsert;
        synchronized (PAGE_BUFFER) {
            batchToInsert = new ArrayList<>(PAGE_BUFFER);
            PAGE_BUFFER.clear();
        }

        createBatchInsertForPage(batchToInsert);
    }

    public void flushRemainingPages() {
        log.info("Sending the last batch insert for page.");
        flush();
    }

    private void createBatchInsertForPage(List<Page> batchToInsert) {
        log.info("Create a batch insert for a {} record page.", batchToInsert.size());
        String pageSql = "INSERT INTO page (path, site_id, code, content) VALUES (?, ?, ?, ?)";
        BatchInserter inserter = new BatchInserter();
        PageStatementSetter setter = new PageStatementSetter();

        inserter.batchInsert(
                batchToInsert,
                pageSql,
                setter,
                dataSource,
                PAGE_BATCH_SIZE,
                new AtomicBoolean()
        );
    }

    public void clearBuffer() {
        PAGE_BUFFER.clear();
    }
}
