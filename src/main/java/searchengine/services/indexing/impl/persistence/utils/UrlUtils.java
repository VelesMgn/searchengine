package searchengine.services.indexing.impl.persistence.utils;

public final class UrlUtils {
    public static String normalizeSiteUrl(String url) {
        return url.replace("www.", "").replaceAll("/$", "");
    }

    public static String extractPath(String fullUrl, String siteBaseUrl) {
        return fullUrl.replace(siteBaseUrl, "");
    }
}
