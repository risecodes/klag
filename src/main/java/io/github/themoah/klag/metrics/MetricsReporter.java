package io.github.themoah.klag.metrics;

import io.github.themoah.klag.model.ConsumerGroupLag;
import io.vertx.core.Future;
import java.util.List;

/**
 * Interface for reporting Kafka lag metrics to external systems.
 */
public interface MetricsReporter {

  /**
   * Reports consumer group lag metrics.
   *
   * @param lagData list of consumer group lag data
   * @return Future that completes when metrics are recorded
   */
  Future<Void> reportLag(List<ConsumerGroupLag> lagData);

  /**
   * Starts the reporter.
   *
   * @return Future that completes when started
   */
  Future<Void> start();

  /**
   * Closes the reporter and releases resources.
   *
   * @return Future that completes when closed
   */
  Future<Void> close();
}
