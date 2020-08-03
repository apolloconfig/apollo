package com.ctrip.framework.apollo.util.yaml;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.parser.ParserException;

import com.ctrip.framework.apollo.core.utils.StringUtils;

/**
 * Transplanted from org.springframework.beans.factory.config.YamlProcessor since apollo can't depend on Spring directly
 *
 * @since 1.3.0
 */
public class YamlParser {
  private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);

  private PropertiesFactory propertiesFactory = ApolloInjector.getInstance(PropertiesFactory.class);

  /**
   * Transform yaml content to properties
   */
  public Properties yamlToProperties(String yamlContent) {
    Yaml yaml = createYaml();
    final Properties result = propertiesFactory.getPropertiesInstance();
    process(new MatchCallback() {
      @Override
      public void process(Properties properties, Map<String, Object> map) {
        result.putAll(properties);
      }
    }, yaml, yamlContent);
    return result;
  }

  /**
   * Create the {@link Yaml} instance to use.
   */
  private Yaml createYaml() {
    return new Yaml(new StrictMapAppenderConstructor());
  }

  private boolean process(MatchCallback callback, Yaml yaml, String content) {
    int count = 0;
    if (logger.isDebugEnabled()) {
      logger.debug("Loading from YAML: " + content);
    }
    for (Object object : yaml.loadAll(content)) {
      if (object != null && process(asMap(object), callback)) {
        count++;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") + " from YAML resource: " + content);
    }
    return (count > 0);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object object) {
    // YAML can have numbers as keys
    Map<String, Object> result = new LinkedHashMap<>();
    if (!(object instanceof Map)) {
      // A document can be a text literal
      result.put("document", object);
      return result;
    }

    Map<Object, Object> map = (Map<Object, Object>) object;
    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        value = asMap(value);
      }
      Object key = entry.getKey();
      if (key instanceof CharSequence) {
        result.put(key.toString(), value);
      } else {
        // It has to be a map key in this case
        result.put("[" + key.toString() + "]", value);
      }
    }
    return result;
  }

  private boolean process(Map<String, Object> map, MatchCallback callback) {
    Properties properties = propertiesFactory.getPropertiesInstance();
    properties.putAll(getFlattenedMap(map));

    if (logger.isDebugEnabled()) {
      logger.debug("Merging document (no matchers set): " + map);
    }
    callback.process(properties, map);
    return true;
  }

  private Map<String, Object> getFlattenedMap(Map<String, Object> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    buildFlattenedMap(result, source, null);
    return result;
  }

  private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      if (!StringUtils.isBlank(path)) {
        if (key.startsWith("[")) {
          key = path + key;
        } else {
          key = path + '.' + key;
        }
      }
      Object value = entry.getValue();
      if (value instanceof String) {
        result.put(key, value);
      } else if (value instanceof Map) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        buildFlattenedMap(result, map, key);
      } else if (value instanceof Collection) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) value;
        int count = 0;
        for (Object object : collection) {
          buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
        }
      } else {
        result.put(key, (value != null ? value.toString() : ""));
      }
    }
  }

  private interface MatchCallback {
    void process(Properties properties, Map<String, Object> map);
  }

  /**
   * A specialized {@link SafeConstructor} that checks for duplicate keys.
   */
  private static class StrictMapAppenderConstructor extends SafeConstructor {

    // Declared as public for use in subclasses
    StrictMapAppenderConstructor() {
      super();
    }

    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
      try {
        return super.constructMapping(node);
      } catch (IllegalStateException ex) {
        throw new ParserException("while parsing MappingNode", node.getStartMark(), ex.getMessage(), node.getEndMark());
      }
    }

    @Override
    protected Map<Object, Object> createDefaultMap() {
      final Map<Object, Object> delegate = super.createDefaultMap();
      return new AbstractMap<Object, Object>() {
        @Override
        public Object put(Object key, Object value) {
          if (delegate.containsKey(key)) {
            throw new IllegalStateException("Duplicate key: " + key);
          }
          return delegate.put(key, value);
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
          return delegate.entrySet();
        }
      };
    }
  }

}
