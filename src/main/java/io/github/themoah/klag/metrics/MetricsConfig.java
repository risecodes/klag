package io.github.themoah.klag.metrics;

/**
 * Configuration for metrics collection and reporting.
 */
public record MetricsConfig(
  String reporterType,
  long collectionIntervalMs,
  String consumerGroupFilter
) {

  private static final String DEFAULT_REPORTER = "none";
  private static final long DEFAULT_INTERVAL_MS = 60_000L;
  private static final String DEFAULT_FILTER = "*";

  /**
   * Loads configuration from environment variables.
   */
  public static MetricsConfig fromEnvironment() {
    String reporter = System.getenv().getOrDefault("METRICS_REPORTER", DEFAULT_REPORTER);
    long interval = parseLong("METRICS_INTERVAL_MS", DEFAULT_INTERVAL_MS);
    String filter = System.getenv().getOrDefault("METRICS_GROUP_FILTER", DEFAULT_FILTER);

    return new MetricsConfig(reporter, interval, filter);
  }

  /**
   * Returns true if metrics reporting is enabled.
   */
  public boolean isEnabled() {
    return reporterType != null && !reporterType.isBlank() && !reporterType.equals("none");
  }

  private static long parseLong(String envVar, long defaultValue) {
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
