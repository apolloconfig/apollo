在2.4.0版本的java客户端中，增加了指标收集,导出的支持，用户可以自行扩展接入不同的监控系统。

## 以接入Prometheus为例

创建PrometheusApolloClientMetricsExporter类，继承AbstractApoolloClientMetircsExporter(通用指标导出框架)

继承后大致代码如下

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

doInit方法是供用户在初始化时自行做扩展的，会在AbstractApoolloClientMetircsExporter里的init方法被调用

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

这里引入了prometheus的java客户端，需要对CollectorRegistry和缓存map做初始化

```java
  private CollectorRegistry registry;
  private  Map<String, Collector.Describable> map;

  @Override
  public void doInit() {
    registry = new CollectorRegistry();
    map = new HashMap<>();
  }
```

isSupport方法将会在DefaultApolloClientMetricsExporterFactory通过SPI读取MetricsExporter时被调用做判断，用于实现在有多个SPI实现时可以准确启用用户所配置的那一个Exporter

比如配置时候你希望启用prometheus，你规定的值为prometheus，那这里就同步

```java
  @Override
  public boolean isSupport(String form) {
    return PROMETHEUS.equals(form);
  }
```

registerOrUpdateCounterSample,registerOrUpdateGaugeSample即是用来注册Counter,Gauge类型指标的方法，只需要根据传来的参数正常注册即可

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

最后需要实现response方法，该方法用于导出你接入的监控系统格式的数据，最终会在ConfigMonitor的getExporterData方法里得到，用于用户自行暴露端口然后供监控系统拉取

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

至此，已经将Client的指标数据接入Prometheus。

完整代码：TODO URL