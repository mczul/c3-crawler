package de.cronoscx.contests.crawler.strategies;

import de.cronoscx.contests.crawler.core.Assessor;
import de.cronoscx.contests.crawler.core.Crawler;
import de.cronoscx.contests.crawler.core.Scout;
import de.cronoscx.contests.crawler.core.Scout.Report;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class StructuredConcurrencyCrawler extends Crawler {
    private final Scout scout = new Scout(memory);
    private final Assessor assessor = new Assessor();

    @Override
    public Optional<URI> dig(URI source, String query) throws InterruptedException {
        return dig(List.of(source), query);
    }

    private static class OptionalTaskScope<T> extends StructuredTaskScope<Optional<T>> {
        private final AtomicReference<T> result = new AtomicReference<>();

        @Override
        protected void handleComplete(Subtask<? extends Optional<T>> subtask) {
            switch (subtask.state()) {
                case UNAVAILABLE -> throw new IllegalStateException();
                case SUCCESS -> {
                    Optional<T> subtaskResult = subtask.get();
                    if (subtaskResult.isPresent()) {
                        result.set(subtaskResult.get());
                        super.shutdown();
                    }
                }
            }
        }

        public Optional<T> result() {
            return Optional.ofNullable(result.get());
        }
    }


    public Optional<URI> dig(Collection<URI> sources, String query) throws InterruptedException {
        try (var scope = new OptionalTaskScope<URI>()) {
            for (URI source : sources) {
                scope.fork(() -> {
                    LOG.info("Checking %s".formatted(source));
                    Report report = scout.check(source, query);

                    if (report.found()) {
                        return Optional.of(source);
                    }

                    Set<URI> unknownReferences = report.references()
                        .stream()
                        .filter(memory::unknown)
                        .filter(assessor::relevant)
                        .collect(Collectors.toSet());
                    if (unknownReferences.isEmpty()) {
                        return Optional.empty();
                    }

                    return dig(unknownReferences, query);
                });
            }

            scope.join();

            return scope.result();
        }
    }

    @Override
    public void close() {
    }
}
