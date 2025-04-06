package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Long> {
    Page findByPath(String path);
    Integer countPageBySiteId(Site siteId);

    @Query(
            value = "SELECT COUNT(*) FROM page",
            nativeQuery = true
    )
    Integer countAllPage();

    @Query(
            value = "SELECT COUNT(*) FROM page WHERE site_id = :siteId",
            nativeQuery = true
    )
    Long countBySite(Long siteId);

    Page getPagesById(long id);
}
