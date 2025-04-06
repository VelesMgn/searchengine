package searchengine.services.indexing.impl.persistence.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.services.indexing.impl.persistence.batch.BatchInserter;
import searchengine.services.indexing.impl.persistence.batch.setters.LemmaStatementSetter;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public final class LemmaService {
    private static final List<Lemma> LEMMA_BUFFER = Collections.synchronizedList(new ArrayList<>());
    private static final int BATCH_SIZE_FOR_LEMMA = 1000;

    private final LemmaRepository lemmaRepository;
    private final DataSource dataSource;

    public void saveAll(List<Lemma> lemmasToUpdate) {
        lemmaRepository.saveAll(lemmasToUpdate);
    }

    public Lemma saveOrUpdate(Site site, String lemmaText) {
        lemmaRepository.insertOrUpdateLemma(lemmaText, site.getId());
        return lemmaRepository.findBySiteIdAndLemma(site, lemmaText);
    }

    public List<Lemma> findLemmas(Set<String> lemmas) {
        return lemmaRepository.findByLemmaIn(lemmas);
    }

    public void deleteAll() {
        lemmaRepository.deleteAll();
    }

    public Lemma addLemma(Site site, String lemmaText, int frequency) {
        Lemma lemma = buildLemma(site, lemmaText, frequency);
        LEMMA_BUFFER.add(lemma);

        return lemma;
    }

    private Lemma buildLemma(Site site, String lemmaText, int frequency) {
        return Lemma.builder()
                .lemma(lemmaText)
                .siteId(site)
                .frequency(frequency)
                .build();
    }

    public void createBatchInsertForLemma(AtomicBoolean stopRequested) {
        log.info("Create batch insert for lemma");
        String lemmaSql = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (?, ?, ?)";
        BatchInserter inserter = new BatchInserter();
        LemmaStatementSetter setter = new LemmaStatementSetter();

        inserter.batchInsert(
                LEMMA_BUFFER,
                lemmaSql,
                setter,
                dataSource,
                BATCH_SIZE_FOR_LEMMA,
                stopRequested
        );
    }

    public Map<Lemma, Lemma> updatingTheDuplicateKey() {
        Map<Lemma, Long> grouped = LEMMA_BUFFER.stream()
                .collect(Collectors.groupingBy(
                        lemma -> buildLemma(lemma.getSiteId(), lemma.getLemma(), 0),
                        Collectors.counting()
                ));

        Map<Lemma, Lemma> oldToNewLemmaMap = new HashMap<>();

        for (Lemma original : LEMMA_BUFFER) {
            Lemma key = buildLemma(original.getSiteId(), original.getLemma(), 0);
            long frequency = grouped.get(key);
            oldToNewLemmaMap.put(original, buildLemma(original.getSiteId(), original.getLemma(), (int) frequency));
        }

        LEMMA_BUFFER.clear();
        LEMMA_BUFFER.addAll(oldToNewLemmaMap.values());

        return oldToNewLemmaMap;
    }

    public void deleteAllFromList(List<Lemma> lemmasToDelete) {
        lemmaRepository.deleteAll(lemmasToDelete);
    }

    public void clearBuffer() {
        LEMMA_BUFFER.clear();
    }
}
