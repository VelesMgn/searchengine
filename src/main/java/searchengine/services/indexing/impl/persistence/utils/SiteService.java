package searchengine.services.indexing.impl.persistence.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SiteFromList;
import searchengine.config.SitesListConfig;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public final class SiteService {
    private final SiteRepository siteRepository;

    public Site save(SiteFromList siteFromList) {
        return siteRepository.save(
                Site.builder()
                        .name(siteFromList.getName())
                        .url(UrlUtils.normalizeSiteUrl(siteFromList.getUrl()))
                        .status(Status.INDEXING)
                        .statusTime(LocalDateTime.now())
                        .build()
        );
    }

    public void updateStatus(long id, Status status, String error) {
        siteRepository.findById(id).ifPresent(site -> {
            site.setStatus(status);
            site.setLastError(error);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
    }

    public Site findById(long id) {
        return siteRepository.findById(id).orElse(null);
    }

    public Site getSiteForOnePage(String path, SitesListConfig sitesFromConfigFile) {
        return getSiteOnPagePath(path)
                .or(() -> sitesFromConfigFile.getSites().stream()
                        .filter(siteFromList ->
                                path.startsWith(UrlUtils.normalizeSiteUrl(siteFromList.getUrl())))
                        .findFirst()
                        .map(this::save))
                .orElse(null);
    }

    private Optional<Site> getSiteOnPagePath(String path) {
        return siteRepository.findAll().stream()
                .filter(site -> path.startsWith(site.getUrl()))
                .findFirst();
    }

    public void deleteAll() {
        siteRepository.deleteAll();
    }
}
