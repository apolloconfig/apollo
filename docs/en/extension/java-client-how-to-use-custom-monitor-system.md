In version 2.4.0 and above of the Java client, support for metrics collection and export has been added, allowing users to extend and integrate with different monitoring systems.

## Taking Prometheus Integration as an Example

Create the `PrometheusApolloClientMetricsExporter` class, which extends `AbstractApolloClientMetricsExporter` (the generic metrics export framework).

The code after extending is roughly as follows:

```java

public class PrometheusApolloClientMetricsExporter extends
    AbstractApolloClientMetricsExporter implements ApolloClientMetricsExporter {

 @Override
 public void doInit() {

 }

 @Override
 public boolean isSupport(String form) {

 }


 @Override
 public void registerOrUpdateCounterSample(String name, Map<String, String> tags, double incrValue) {

 }


 @Override
 public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {

 }

  @Override
 public String response() {

 }
}

```

The doInit method is provided for users to extend during initialization and will be called in the init method of AbstractApolloClientMetricsExporter.

```java
  @Override
  public void init(List<ApolloClientMonitorEventListener> collectors, long collectPeriod) {
    log.info("Initializing metrics exporter with {} collectors and collect period of {} seconds.",
        collectors.size(), collectPeriod);
    doInit();
    this.collectors = collectors;
    initScheduleMetricsCollectSync(collectPeriod);
    log.info("Metrics collection scheduled with a period of {} seconds.", collectPeriod);
  }
```

Here, the Prometheus Java client is introduced, and the CollectorRegistry and cache map need to be initialized.

```java
  private CollectorRegistry registry;
  private  Map<String, Collector.Describable> map;

  @Override
  public void doInit() {
    registry = new CollectorRegistry();
    map = new HashMap<>();
  }
```

The isSupport method will be called in DefaultApolloClientMetricsExporterFactory via SPI to check which MetricsExporter to enable, allowing accurate activation of the configured exporter when multiple SPI implementations exist.

For example, if you want to enable Prometheus and specify the value as "prometheus," it should synchronize here:

```java
  @Override
  public boolean isSupport(String form) {
    return PROMETHEUS.equals(form);
  }
```

The methods registerOrUpdateCounterSample and registerOrUpdateGaugeSample are used to register Counter and Gauge type metrics, simply registering based on the provided parameters.

```java
  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags,
      double incrValue) {
    Counter counter = (Counter) map.get(name);
    if (counter == null) {
      counter = createCounter(name, tags);
      map.put(name, counter);
    }
    counter.labels(tags.values().toArray(new String[0])).inc(incrValue);
  }

  private Counter createCounter(String name, Map<String, String> tags) {
    return Counter.build()
        .name(name)
        .help("apollo")
        .labelNames(tags.keySet().toArray(new String[0]))
        .register(registry);
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    Gauge gauge = (Gauge) map.get(name);
    if (gauge == null) {
      gauge = createGauge(name, tags);
      map.put(name, gauge);
    }
    gauge.labels(tags.values().toArray(new String[0])).set(value);
  }

  private Gauge createGauge(String name, Map<String, String> tags) {
    return Gauge.build()
        .name(name)
        .help("apollo")
        .labelNames(tags.keySet().toArray(new String[0]))
        .register(registry);
  }
```

Finally, you need to implement the response method, which is used to export data in the format of the integrated monitoring system. It will ultimately be obtained in the getExporterData method of ConfigMonitor, allowing users to expose an endpoint for monitoring systems to pull data.

```java
  @Override
  public String response() {
    try (StringWriter writer = new StringWriter()) {
      TextFormat.writeFormat(TextFormat.CONTENT_TYPE_OPENMETRICS_100, writer,
          registry.metricFamilySamples());
      return writer.toString();
    } catch (IOException e) {
      logger.error("Write metrics to Prometheus format failed", e);
      return "";
    }
  }
```

At this point, the client's metric data has been integrated with Prometheus.

Full code：[code](https://github.com/apolloconfig/apollo-java/main/master/apollo-plugin/apollo-plugin-client-prometheus/src/main/java/com/ctrip/framework/apollo/monitor/internal/exporter/impl/PrometheusApolloClientMetricsExporter.java)