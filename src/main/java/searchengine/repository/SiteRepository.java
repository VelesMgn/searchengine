package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<Site, Long> {
    Site findSiteByUrl(String url);

    Site getSiteById(long id);
}
