package io.github.themoah.klag.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Micrometer registries.
 */
public final class MicrometerConfig {

  private static final Logger log = LoggerFactory.getLogger(MicrometerConfig.class);

  private MicrometerConfig() {}

  /**
   * Creates a Datadog meter registry configured from environment variables.
   */
  public static MeterRegistry createDatadogRegistry() {
    log.info("Creating Datadog meter registry");

    DatadogConfig config = new DatadogConfig() {
      @Override
      public String apiKey() {
        return System.getenv("DD_API_KEY");
      }

      @Override
      public String applicationKey() {
        return System.getenv("DD_APP_KEY");
      }

      @Override
      public String uri() {
        String site = System.getenv().getOrDefault("DD_SITE", "datadoghq.com");
        return "https://api." + site;
      }

      @Override
      public String get(String key) {
        return null;
      }
    };

    return new DatadogMeterRegistry(config, Clock.SYSTEM);
  }

  /**
   * Creates a Prometheus meter registry.
   */
  public static PrometheusMeterRegistry createPrometheusRegistry() {
    log.info("Creating Prometheus meter registry");
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  /**
   * Creates a meter registry based on the reporter type.
   *
   * @param reporterType the type of reporter ("datadog", "prometheus", etc.)
   * @return the configured MeterRegistry, or null if type is unknown
   */
  public static MeterRegistry createRegistry(String reporterType) {
    if (reporterType == null) {
      return null;
    }

    return switch (reporterType.toLowerCase()) {
      case "datadog" -> createDatadogRegistry();
      case "prometheus" -> createPrometheusRegistry();
      default -> {
        log.warn("Unknown reporter type: {}", reporterType);
        yield null;
      }
    };
  }

  /**
   * Binds JVM metrics (memory, GC, threads, CPU) to the given registry.
   *
   * @param registry the meter registry to bind JVM metrics to
   */
  public static void bindJvmMetrics(MeterRegistry registry) {
    log.info("Binding JVM metrics to registry");
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
  }
}
