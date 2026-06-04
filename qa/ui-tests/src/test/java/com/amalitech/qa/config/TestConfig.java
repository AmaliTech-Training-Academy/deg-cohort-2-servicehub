package com.amalitech.qa.config;

/**
 * All environment-level knobs in one place.
 * Override via system property: mvn test -Dbase.url=http://staging:4200
 */
public final class TestConfig {

    public static final String BASE_URL =
            System.getProperty("base.url", "http://localhost:4200");

    public static final String BACKEND_URL =
            System.getProperty("backend.url", "http://localhost:8080");

    /** Default explicit-wait timeout for element readiness checks. */
    public static final int WAIT_TIMEOUT_SECONDS = 10;

    /** Page-load and navigation timeout. */
    public static final int PAGE_LOAD_TIMEOUT_SECONDS = 15;

    private TestConfig() {}
}
