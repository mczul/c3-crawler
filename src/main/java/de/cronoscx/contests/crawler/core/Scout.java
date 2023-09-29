package de.cronoscx.contests.crawler.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.util.MimeTypeUtils;

public final class Scout implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger("Scout");
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    public record Report(URI source, String query, Boolean found, List<URI> references, Integer responseSize) {
    }

    private final HttpClient client = HttpClient.newBuilder()
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
    URI parse(String href, URI source, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<URI> baseHref) {
        try {
            return baseHref.map(base -> base.resolve(href)).orElse(source.resolve(href));
        } catch (Throwable th) {
            final var message = baseHref.map(
                    base -> "\uD83D\uDCA5 failed to parse href \"%s\" with base \"%s\"".formatted(href, base))
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
                    if (response.headers()
                        .allValues("content-type")
                        .stream()
                        .map(MimeTypeUtils::parseMimeType)
                        .noneMatch(MimeTypeUtils.TEXT_HTML::isCompatibleWith)) {
                        return new Report(uri, query, false, List.of(), responseSize);
                    }

                    // query contained in response body?
                    final String body = response.body();
                    if (body.contains(query)) {
                        return new Report(uri, query, true, List.of(), responseSize);
                    }

                    // extract links contained in current response body
                    Document document = Jsoup.parse(body, uri.toASCIIString());
                    Elements anchors = document.select("a[href]");
                    List<URI> references = anchors.stream().filter(a -> {
                            String href = a.attr("href");
                            return !href.startsWith("#") && !href.startsWith("mailto") && !href.startsWith("javascript");
                        })
                        // This resolves the href against the base url, see https://jsoup.org/cookbook/extracting-data/working-with-urls
                        .map(a -> a.absUrl("href"))//
                        .map(Scout::toURI)//
                        .map(Scout::withoutFragment)//
                        .sorted()//
                        .distinct()//
                        .toList();
                    return new Report(uri, query, false, references, responseSize);

                } catch (Throwable t) {
                    LOG.warning("\uD83D\uDCA5 failed request to %s (%s)".formatted(uri, t.toString()));
                    return new Report(uri, query, false, List.of(), 0);
                }
            });
        } finally {
            memory.release(source);
        }
    }

    private static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse '" + uri + "'", e);
        }
    }

    private static URI withoutFragment(URI uri) {
        try {
            return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to remove fragment part for uri '" + uri + "'", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }

}
