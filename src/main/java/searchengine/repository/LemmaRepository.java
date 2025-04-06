package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Lemma findBySiteIdAndLemma(Site siteId, String lemma);

    List<Lemma> findByLemmaIn(Collection<String> lemmas);

    @Modifying
    @Query(value = """
        INSERT INTO lemma (lemma, site_id, frequency)
        VALUES (:lemma, :siteId, 1)
        ON DUPLICATE KEY UPDATE frequency = frequency + 1
        """, nativeQuery = true)
    void insertOrUpdateLemma(@Param("lemma") String lemmaText, @Param("siteId") Long site);

    @Query(
            value = "SELECT COUNT(*) FROM lemma WHERE site_id = :siteId",
            nativeQuery = true
    )
    Long countBy(@Param("siteId") Long siteId);

    @Query(
            value = "SELECT frequency FROM lemma WHERE lemma = :lemma AND site_id = :siteId",
            nativeQuery = true
    )
    Integer getFrequencyOnSite(@Param("lemma") String lemma, @Param("siteId") Long siteId);

    @Query(
            value = "SELECT SUM(frequency) FROM lemma WHERE lemma = :lemma",
            nativeQuery = true
    )
    Integer getFrequency(@Param("lemma") String lemma);

    @Query(
            value = "SELECT id FROM lemma WHERE lemma = :lemma",
            nativeQuery = true
    )
    Set<Long> findIdsByLemma(@Param("lemma") String lemma);

    @Query(
            value = "SELECT id FROM lemma WHERE lemma = :lemma AND site_id = :siteId",
            nativeQuery = true
    )
    Integer findIdByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId")Long siteId);

    @Query(
            value = "SELECT id FROM lemma WHERE lemma = :lemma",
            nativeQuery = true
    )
    Set<Long> findAllIdsByLemma(@Param("lemma") String lemma);
}
