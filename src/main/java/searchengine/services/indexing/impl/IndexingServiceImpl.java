package searchengine.services.indexing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SiteFromList;
import searchengine.config.SitesListConfig;
import searchengine.config.UserAgentAndRefererConfig;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.impl.parser.SinglePageIndexing;

import searchengine.services.indexing.impl.parser.node.Node;
import searchengine.services.indexing.impl.parser.PageCrawlerTask;
import searchengine.services.indexing.impl.persistence.IndexingCoordinator;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public final class IndexingServiceImpl implements IndexingService {
    private final static int FINAL_TASKS = 1;

    private final ConcurrentHashMap<Long, ForkJoinPool> activePools = new ConcurrentHashMap<>();
    private final AtomicBoolean waitExecutorShutdown = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicInteger remainingTasks = new AtomicInteger();

    private final SitesListConfig sitesFromConfigFile;
    private final UserAgentAndRefererConfig config;
    private final IndexingCoordinator coordinator;

    private long measuringCodeExecutionTime;
    private ExecutorService siteExecutor;

    @Override
    public boolean startIndexing() {
        if(remainingTasks.get() != 0 || waitExecutorShutdown.get()) return false;
        initializeIndexing();

        sitesFromConfigFile.getSites()
                .forEach(siteFromList -> siteExecutor.submit(() -> startingAThreadPool(siteFromList)));
        return true;
    }

    private void initializeIndexing() {
        int numberOfSitesForParsing = sitesFromConfigFile.getSites().size();
        measuringCodeExecutionTime = System.currentTimeMillis();

        remainingTasks.addAndGet(numberOfSitesForParsing + FINAL_TASKS);
        stopRequested.set(false);
        PageCrawlerTask.clearAllLinks();
        coordinator.clearAllBuffers();
        coordinator.deleteDataAboutSites();

        siteExecutor = Executors.newFixedThreadPool(numberOfSitesForParsing);
    }

    private void startingAThreadPool(SiteFromList siteFromList) {
        Site site = coordinator.getSiteFromListAndSave(siteFromList);
        Node root = new Node(config, site, site.getUrl());
        PageCrawlerTask task = new PageCrawlerTask(coordinator, stopRequested, root);
        ForkJoinPool pool = new ForkJoinPool();

        activePools.put(site.getId(), pool);
        pool.invoke(task);

        countingCompletedThreadPools();
    }

    private void countingCompletedThreadPools() {
        remainingTasks.decrementAndGet();

        if(remainingTasks.get() == FINAL_TASKS) {
            coordinator.addLemmaAndIndexToTheDatabase(stopRequested);
            completeSiteIndexing();
        }
    }

    private void completeSiteIndexing() {
        activePools.forEach((siteId, pool) -> {
            if(!Status.FAILED.equals(coordinator.getSiteById(siteId).getStatus())) {
                coordinator.saveFinalSiteData(siteId, Status.INDEXED, "");
            }
        });

        remainingTasks.decrementAndGet();
        activePools.clear();
        log.info("Sites parsing took: {} seconds", (System.currentTimeMillis() - measuringCodeExecutionTime) / 1000);
    }

    @Override
    public boolean stopIndexing() {
        if(activePools.isEmpty()) return false;

        String info = "Indexing stopped by the user.";
        stopRequested.set(true);

        activePools.forEach((siteId, pool) -> {
            pool.shutdownNow();
            coordinator.saveFinalSiteData(siteId, Status.FAILED, info);
        });

        shutdownSiteExecutorAsync();
        activePools.clear();
        remainingTasks.set(0);

        log.info(info);
        return true;
    }

    @Override
    public boolean indexPage(String path) {
        if(!activePools.isEmpty()) return false;
        if(path == null || path.isBlank()) return false;

        Site site = coordinator.getSite(path, sitesFromConfigFile);
        if(site == null) return false;

        return new SinglePageIndexing(coordinator, site).getIndexPageResult(path);
    }

    private void shutdownSiteExecutorAsync() {
        if(waitExecutorShutdown.getAndSet(true)) return;

        CompletableFuture.runAsync(() -> {
            log.info("Shutting down siteExecutor.");
            siteExecutor.shutdown();

            try {
                if(!siteExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("SiteExecutor.shutdown timed out. Forcing shutdownNow.");
                    siteExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted during siteExecutor shutdown, forcing shutdownNow.", e);
                siteExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            waitExecutorShutdown.set(false);
            log.info("siteExecutor shutdown process finished.");
        });
    }
}