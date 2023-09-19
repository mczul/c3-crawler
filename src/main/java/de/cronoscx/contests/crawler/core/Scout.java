package de.cronoscx.contests.crawler.core;

import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class Scout implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger("Scout");
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    public record Report(URI source, String query, Boolean found, List<URI> references, Integer responseSize) {
    }

    private static final Pattern BASE_HREF_PATTERN = Pattern.compile(
            "<base\s+href=\"(?<href>[^\"?#\s]+)/?\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HYPERLINK_PATTERN = Pattern.compile(
            "<a\s+[^>]*href=\"(?<href>[^\"?#\s]+)/?\"",
            Pattern.CASE_INSENSITIVE
    );
    private final HttpClient client = HttpClient
            .newBuilder()
            .executor(executor)
            .version(Crawler.HTTP_VERSION)
            .connectTimeout(Duration.ofSeconds(Crawler.TIMEOUT_SECONDS))
            .followRedirects(Redirect.ALWAYS)
            .build();
    private final Memory memory;

    public Scout(Memory memory) {
        this.memory = memory;
    }

    /**
     * Transforms the given {@code href} based on source and base URL to form an absolute URI. Returns {@code null} if
     * parsing fails.
     */
    URI parse(String href,
              URI source,
              @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<URI> baseHref) {
        try {
            return baseHref
                    .map(base -> base.resolve(href))
                    .orElse(source.resolve(href));
        } catch (Throwable th) {
            final var message = baseHref.map(base -> "\uD83D\uDCA5 failed to parse href \"%s\" with base \"%s\"".formatted(href, base))
                    .orElse("\uD83D\uDCA5 failed to parse href \"%s\"".formatted(href));
            LOG.warning(message);
            return null;
        }
    }

    /**
     * Checks the given source for the existence of the given query string.
     */
    public Report check(URI source, String query) {
        try {
            memory.lock(source);
            // check for previous results
            return memory.findOrCall(source, uri -> {
                try {
                    // fetch data
                    final var request = HttpRequest.newBuilder(uri)
                            .version(Crawler.HTTP_VERSION)
                            .timeout(Duration.ofSeconds(Crawler.TIMEOUT_SECONDS))
                            .GET()
                            .build();

                    // wait for response
                    final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    final var responseSize = response.body().length();

                    // invalid response
                    if (response.statusCode() != 200) {
                        return new Report(uri, query, false, List.of(), responseSize);
                    }
                    if (response.headers().allValues("content-type").stream()
                            .map(MimeTypeUtils::parseMimeType)
                            .noneMatch(MimeTypeUtils.TEXT_HTML::isCompatibleWith)) {
                        return new Report(uri, query, false, List.of(), responseSize);
                    }

                    // query containted in response body?
                    final var body = response.body();
                    if (body.contains(query)) {
                        return new Report(uri, query, true, List.of(), responseSize);
                    }

                    // extract base url if present
                    final var baseHrefMatcher = BASE_HREF_PATTERN.matcher(response.body());
                    final var baseHref = baseHrefMatcher.results()
                            .findFirst()
                            .map(match -> match.group("href"))
                            .map(href -> parse(href, source, Optional.empty()))
                            .filter(Objects::nonNull);

                    // extract other links contained in current response body
                    final var hyperlinkMatcher = HYPERLINK_PATTERN.matcher(response.body());
                    final var references = hyperlinkMatcher.results()
                            .map(match -> match.group("href"))
                            .filter(href -> !href.startsWith("#")) // no anchors to eliminate redundant processing
                            .filter(href -> !href.startsWith("mailto")) // no mailto links
                            .filter(href -> !href.startsWith("javascript")) // no scripts
                            .map(href -> parse(href, source, baseHref))
                            .filter(Objects::nonNull)
                            .sorted()
                            .distinct()
                            .toList();
                    return new Report(uri, query, false, references, responseSize);

                } catch (Throwable ignored) {
                    LOG.warning("\uD83D\uDCA5 failed request to %s".formatted(uri));
                    return new Report(uri, query, false, List.of(), 0);
                }
            });
        } finally {
            memory.release(source);
        }
    }

    @Override
    public void close() {
        client.close();
    }

}
