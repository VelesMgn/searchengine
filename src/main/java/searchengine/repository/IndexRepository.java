package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO `index` (page_id, lemma_id, `rank`)
        VALUES (:pageId, :lemmaId, :rank)
        """, nativeQuery = true)
    void insertIndex(@Param("pageId") Page pageId,
                             @Param("lemmaId") Lemma lemmaId,
                             @Param("rank") float rank);

    void deleteByLemmaId(Lemma lemmaId);

    @Query(
            value = "SELECT page_id FROM `index` WHERE lemma_id = :lemmaId",
            nativeQuery = true
    )
    Set<Long> getPageIdsByLemmaId(@Param("lemmaId") Long lemmaId);

    @Query(
            value = "SELECT `rank` FROM `index` WHERE page_id = :pageId AND lemma_id = :lemmaId",
            nativeQuery = true
    )
    Float getRankByLemmaIdAndPageId(@Param("pageId") long pageId, @Param("lemmaId") long lemmaId);

    @Query(
            value = "SELECT page_id FROM `index` WHERE lemma_id = :lemmaId AND site_id = :siteId",
            nativeQuery = true
    )
    Set<Long> getPageIdsByLemmaIdAndSiteId(@Param("lemmaId")long lemmaId, @Param("siteId")long siteId);

    @Query(
            value = "SELECT page_id FROM `index` WHERE lemma_id IN (:lemmaIds)",
            nativeQuery = true
    )
    Set<Long> getPageIdsByLemmaIds(@Param("lemmaIds") Set<Long> lemmaIds);

    @Query(
            value = "SELECT SUM(`rank`) FROM `index` WHERE page_id = :pageId AND lemma_id IN (:lemmaIds)",
            nativeQuery = true
    )
    Float getTotalRankByPageIdAndLemmaIds(@Param("pageId") long pageId, @Param("lemmaIds") Set<Long> lemmaIds);
}
