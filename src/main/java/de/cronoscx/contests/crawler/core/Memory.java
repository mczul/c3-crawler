package de.cronoscx.contests.crawler.core;

import de.cronoscx.contests.crawler.core.Scout.Report;
import java.net.URI;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Memorizes previously found {@link Scout.Report}s and provides basic information to analyze progress or history.
 * Provides additional synchronization for specific sources and must be used to prevent redundant processing.
 */
public final class Memory {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.GERMAN);
    private final Map<URI, Report> history = new ConcurrentHashMap<>();
    private final ConcurrentMap<URI, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Represents the number of known {@link Scout.Report}s
     */
    public Long size() {
        return (long) history.size();
    }

    /**
     * Returns {@code true} if given source is currently locked or a corresponding history entry is present;
     * {@code false} otherwise
     */
    public boolean unknown(URI source) {
        return !locks.containsKey(source) && !history.containsKey(source);
    }

    /**
     * Acquire lock for given source to prevent redundant execution
     */
    public void lock(URI source) {
        locks.computeIfAbsent(source, (uri) -> new ReentrantLock()).lock();
    }

    /**
     * Release lock for given source
     */
    public synchronized void release(URI source) {
        locks.get(source).unlock();
        locks.remove(source);
    }

    /**
     * Provides memorization... returns previously generated report for known sources or calls the given handler
     * otherwise (which then stores the result within memory).
     */
    public Scout.Report findOrCall(URI source, Function<URI, Scout.Report> handler) {
        Report historyReport = history.get(source);
        if (historyReport != null) {
            return historyReport;
        }

        Report report = handler.apply(source);
        history.put(source, report);
        return report;
    }

    /**
     * Returns a "pretty" string with statistical key facts
     */
    public String statistics() {
        return """
            # sources: %s
            # references: %s
                # characters parsed: %s""".formatted(//
            NUMBER_FORMAT.format(history.size()),//
            NUMBER_FORMAT.format(history.values().stream()//
                .map(Scout.Report::references)//
                .map(List::size)//
                .reduce(0, Integer::sum)),//
            NUMBER_FORMAT.format(history.values().stream()//
                .map(Scout.Report::responseSize)//
                .map(Long::valueOf)//
                .reduce(0L, Long::sum))//
        );
    }

}
