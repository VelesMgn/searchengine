package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.exception.ApplicationException;

import java.util.List;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesListConfig {
    private List<SiteFromList> sites;

    public List<SiteFromList> getSites() {

        if (sites.isEmpty()) {
            log.error("The list of sites from the configuration file is empty.");
            throw new ApplicationException("The list of sites cannot be empty.");
        }

        if (sites.stream().anyMatch(s -> s.getName().isBlank() || s.getUrl().isBlank())) {
            log.error("The field with the site name or URL should not be empty.");
            throw new ApplicationException("The field with the site name or URL is empty.");
        }

        return sites;
    }
}
