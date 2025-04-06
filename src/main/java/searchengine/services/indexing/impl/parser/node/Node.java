package searchengine.services.indexing.impl.parser.node;

import lombok.Getter;
import searchengine.config.UserAgentAndRefererConfig;
import searchengine.model.Site;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public final class Node {
    private final UserAgentAndRefererConfig config;
    private final List<Node> children;
    private final String parentUrl;
    private final Site site;

    public Node(UserAgentAndRefererConfig config, Site site, String parentUrl) {
        this.config = config;
        this.site = site;
        this.parentUrl = parentUrl;

        children = new CopyOnWriteArrayList<>();
    }

    public void addChildren(Node node) {
        children.add(node);
    }
}
