package de.cronoscx.contests.crawler.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class Crawler implements AutoCloseable {
    public static final HttpClient.Version HTTP_VERSION = HttpClient.Version.HTTP_1_1;
    public static final int TIMEOUT_SECONDS = 10;

    protected static final Logger LOG = Logger.getLogger("Crawler");
    protected final Memory memory = new Memory();
    protected final Assessor assessor = new Assessor();

    /**
     * represents the history of actions that are memorized during digging
     */
    public final Memory memory() {
        return memory;
    }

    /**
     * main functionality.
     *
     * @param source represents the web page where search starts (e.g. "<a href="https://www.cronoscx.de/home">cronoscx.de</a>")
     * @param query  represents the search term (must be containted in document source, no HTML parsing supported!)
     * @return the URI of the first found page containing the query string
     */
    public abstract Optional<URI> dig(URI source, String query) throws InterruptedException;

}
