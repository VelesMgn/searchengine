package searchengine.services.indexing.impl.persistence.utils;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

@Service
@RequiredArgsConstructor
public final class PageService {
    private final PageRepository pageRepository;

    public Page save(Site site, String pageUrl, int statusCode, Document doc) {
        Page page = buildPage(site, pageUrl, statusCode, doc);
        return pageRepository.save(page);
    }

    public Page getPage(String path) {
        return pageRepository.findByPath(path);
    }

    public void deleteById(Long pageId) {
        pageRepository.deleteById(pageId);
    }

    public void deleteAll() {
        pageRepository.deleteAll();
    }

    public Page buildPage(Site site, String pageUrl, int statusCode, Document doc) {
        return Page.builder()
                .siteId(site)
                .path(UrlUtils.extractPath(pageUrl, site.getUrl()))
                .code(statusCode)
                .content(doc.html())
                .build();
    }
}