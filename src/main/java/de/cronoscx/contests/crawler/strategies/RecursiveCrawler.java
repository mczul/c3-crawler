package de.cronoscx.contests.crawler.strategies;

import de.cronoscx.contests.crawler.core.Crawler;
import de.cronoscx.contests.crawler.core.Scout;

import java.net.URI;
import java.util.Optional;

public class RecursiveCrawler extends Crawler {
    private final Scout scout = new Scout(memory);

    @Override
    public Optional<URI> dig(URI source, String query) {
        final Scout.Report report;
        report = scout.check(source, query);
        LOG.info("~~> %s @ %s".formatted(source, Thread.currentThread().getName()));

        // query match found
        if (report.found()) {
            return Optional.of(source);
        }

        // no match: use recursion
        return report.references().parallelStream()
                .filter(assessor::relevant)
                .filter(memory::unknown)
                .map(reference -> dig(reference, query))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    @Override
    public void close() {
        scout.close();
    }
}
