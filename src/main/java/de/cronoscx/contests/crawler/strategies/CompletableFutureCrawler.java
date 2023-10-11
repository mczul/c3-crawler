package de.cronoscx.contests.crawler.strategies;

import de.cronoscx.contests.crawler.core.Crawler;
import de.cronoscx.contests.crawler.core.Scout;
import de.cronoscx.contests.crawler.core.Scout.Report;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CompletableFutureCrawler extends Crawler {
    private final Scout scout = new Scout(memory);
    // private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    // ~~~~~~~~~~~~~~~~~~~~~~~~ 👇 Place magic here 👇 ~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public Optional<URI> dig(URI source, String query) throws InterruptedException {
        CompletableFuture<URI> completableFuture = new CompletableFuture<>();

        executor.execute(() -> dig(source, query, completableFuture));

        try {
            URI result = completableFuture.get();
            executor.shutdownNow();
            return Optional.ofNullable(result);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void dig(URI source, String query, CompletableFuture<URI> future) {
        runningTasks.incrementAndGet();
        try {
            if (!memory.unknown(source)) {
                LOG.finer("Skipping %s, seen already".formatted(source));
                return;
            } else if (!source.getHost().endsWith("cronoscx.de")) {
                LOG.finer("Skipping %s, not on desired host".formatted(source));
                return;
            }

            LOG.finer("Check " + source);
            Report report = scout.check(source, query);
            LOG.finer("Checked " + source);
            if (report.found()) {
                future.complete(source);
            }
            for (URI ref : report.references()) {
                if (Thread.interrupted()) {
                    LOG.info("Thread interrupted, will not recurse further");
                    break;
                }
                executor.execute(() -> dig(ref, query, future));
            }
        } finally {
            int remainingTasks = runningTasks.decrementAndGet();
            LOG.info("Remaining tasks: " + remainingTasks);
            if (remainingTasks == 0) {
                future.complete(null);
            }
        }
    }

    @Override
    public void close() {
        LOG.info("Closing executor");
        executor.shutdownNow();
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~ 👆 Place magic here 👆~~~~~~~~~~~~~~~~~~~~~~~~
}
