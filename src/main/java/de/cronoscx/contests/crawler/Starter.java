package de.cronoscx.contests.crawler;


import de.cronoscx.contests.crawler.core.Assessor;
import de.cronoscx.contests.crawler.core.Crawler;
import de.cronoscx.contests.crawler.strategies.RecursiveCrawler;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

public class Starter {
    public static final String START_URI = "http://www.cronoscx.de";
    private static final Logger LOG = Logger.getLogger("Root");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.GERMAN);
    private static final Assessor ASSESSOR = new Assessor();

    private static Properties loadGitProperties() {
        final var result = new Properties();

        try (final var resource = Starter.class.getResourceAsStream("/git.properties")) {
            result.load(resource);
        } catch (IOException ignored) {
        }

        return result;
    }

    /**
     * Where all begins...
     * <p>
     * First command line argument provided will be the query string to search for.
     */
    public static void main(String... args) throws Exception {
        final StopWatch watch = new StopWatch("Crawler");

        // 🔬
        final var gitProperties = loadGitProperties();
        LOG.info("""
                        \uD83D\uDD2C Java v%s with %d cores
                            ~~> Branch: %s
                            ~~> Commit: %s
                            ~~> Version: %s""".formatted(
                        Runtime.version().toString(),
                        Runtime.getRuntime().availableProcessors(),
                        gitProperties.getProperty("git.branch", "n/a"),
                        gitProperties.getProperty("git.commit.id.abbrev", "n/a"),
                        gitProperties.getProperty("git.build.version", "n/a")
                )
        );

        // 🔎
        final var query = Arrays.stream(args)
                .findFirst()
                .orElse("Door-to-Door");
        LOG.info("\uD83D\uDD0E \"%s\"".formatted(query));

        // 🚀
        LOG.warning("\uD83D\uDE80 at %s".formatted(START_URI));
        watch.start();
        try (final Crawler crawler = new RecursiveCrawler()) {
            // 🛫
            final var uri = new URI(START_URI);
            final Optional<URI> result = crawler.dig(uri, query);

            // 🛬
            watch.stop();
            LOG.warning("\uD83C\uDFC1 after %s ms... %s sources crawled".formatted(
                    NUMBER_FORMAT.format(watch.getTotalTimeMillis()),
                    NUMBER_FORMAT.format(crawler.memory().size())
            ));

            // ⁉️
            final var assessment = result
                    .map(found -> {
                        final var success = ASSESSOR.ok(found, query);
                        if (success) {
                            return "✅ found URI \"%s\" is correct.".formatted(found);
                        } else {
                            return "❎ found URI \"%s\" incorrect.".formatted(found);
                        }
                    })
                    .orElse("❎ no URI found.");
            LOG.warning(assessment);

            // 🔬
            LOG.warning("\uD83D\uDD2C Statistics: \n%s".formatted(crawler.memory().statistics()));
        }

    }

}
