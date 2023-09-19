package de.cronoscx.contests.crawler.strategies;

import de.cronoscx.contests.crawler.core.Crawler;

import java.net.URI;
import java.util.Optional;

public class BetterCrawler extends Crawler {
    // ~~~~~~~~~~~~~~~~~~~~~~~~ 👇 Place magic here 👇 ~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public Optional<URI> dig(URI source, String query) {
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~ 👆 Place magic here 👆~~~~~~~~~~~~~~~~~~~~~~~~
}
