package io.github.themoah.klag.metrics;

import io.github.themoah.klag.model.ConsumerGroupLag;
import io.github.themoah.klag.model.ConsumerGroupLag.PartitionLag;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.Future;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports metrics using Micrometer MeterRegistry.
 * Works with any Micrometer-supported backend (Datadog, Prometheus, etc).
 */
public class MicrometerReporter implements MetricsReporter {

  private static final Logger log = LoggerFactory.getLogger(MicrometerReporter.class);

  private final MeterRegistry registry;
  private final Map<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

  public MicrometerReporter(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Future<Void> reportLag(List<ConsumerGroupLag> lagData) {
    log.debug("Reporting lag metrics for {} consumer groups", lagData.size());

    for (ConsumerGroupLag group : lagData) {
      Tags groupTags = Tags.of("consumer_group", group.consumerGroup());

      // Aggregated lag metrics
      recordGauge("klag.consumer.lag.sum", groupTags, group.totalLag());
      recordGauge("klag.consumer.lag.max", groupTags, group.maxLag());
      recordGauge("klag.consumer.lag.min", groupTags, group.minLag());

      // Per-partition metrics
      for (PartitionLag p : group.partitions()) {
        Tags partitionTags = Tags.of(
          "consumer_group", group.consumerGroup(),
          "topic", p.topic(),
          "partition", String.valueOf(p.partition())
        );

        recordGauge("klag.consumer.lag", partitionTags, p.lag());
        recordGauge("klag.partition.log_end_offset", partitionTags, p.logEndOffset());
        recordGauge("klag.partition.log_start_offset", partitionTags, p.logStartOffset());
        recordGauge("klag.consumer.committed_offset", partitionTags, p.committedOffset());
      }
    }

    return Future.succeededFuture();
  }

  /**
   * Reports topic partition counts.
   */
  public void reportTopicPartitions(Map<String, Integer> topicPartitions) {
    for (var entry : topicPartitions.entrySet()) {
      Tags tags = Tags.of("topic", entry.getKey());
      recordGauge("klag.topic.partitions", tags, entry.getValue());
    }
  }

  @Override
  public Future<Void> start() {
    log.info("MicrometerReporter started");
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> close() {
    log.info("Closing MicrometerReporter");
    if (registry != null) {
      registry.close();
    }
    return Future.succeededFuture();
  }

  private void recordGauge(String name, Tags tags, long value) {
    String key = name + tags.toString();
    AtomicLong atomicValue = gaugeValues.computeIfAbsent(key, k -> {
      AtomicLong newValue = new AtomicLong(value);
      Gauge.builder(name, newValue, AtomicLong::get)
        .tags(tags)
        .register(registry);
      return newValue;
    });
    atomicValue.set(value);
  }
}
