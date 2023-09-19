package de.cronoscx.contests.crawler.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class Assessor {
    private static final List<Predicate<String>> hostRules = List.of(
            Pattern.compile(".*(?:cronoscx).*", Pattern.CASE_INSENSITIVE).asMatchPredicate()
    );
    private static final Logger LOG = Logger.getLogger("Assessor");

    /**
     * Should the given source be considered relevant for digging?
     */
    public boolean relevant(URI source) {
        return hostRules.stream()
                .anyMatch(rule -> rule.test(source.getHost()));
    }

    /**
     * Is the given source a valid page for the given query string?
     */
    public boolean ok(URI result, String query) {
        try (var client = HttpClient
                .newBuilder()
                .version(Crawler.HTTP_VERSION)
                .connectTimeout(Duration.ofSeconds(Crawler.TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
        ) {
            final var request = HttpRequest.newBuilder(result)
                    .version(Crawler.HTTP_VERSION)
                    .timeout(Duration.ofSeconds(Crawler.TIMEOUT_SECONDS))
                    .GET()
                    .build();

            // send request & wait for it (blocking)
            try {
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body().contains(query);
            } catch (IOException e) {
                LOG.warning("\uD83D\uDCA5 failed to verify answer \"%s\"".formatted(result));
                return false;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
