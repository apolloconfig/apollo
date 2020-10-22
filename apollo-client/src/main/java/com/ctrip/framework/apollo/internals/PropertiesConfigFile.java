package com.ctrip.framework.apollo.internals;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.PropertiesUtil;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class PropertiesConfigFile extends AbstractConfigFile {
  protected AtomicReference<String> m_contentCache;

  public PropertiesConfigFile(String namespace,
                              ConfigRepository configRepository) {
    super(namespace, configRepository);
    m_contentCache = new AtomicReference<>();
  }

  @Override
  protected void update(Properties newProperties) {
    m_configProperties.set(newProperties);
    m_contentCache.set(null);
  }

  @Override
  public String getContent() {
    if (m_contentCache.get() == null) {
      m_contentCache.set(doGetContent());
    }
    return m_contentCache.get();
  }

  String doGetContent() {
    if (!this.hasContent()) {
      return null;
    }

    try {
      return PropertiesUtil.toString(m_configProperties.get());
    } catch (Throwable ex) {
      ApolloConfigException exception =
          new ApolloConfigException(String
              .format("Parse properties file content failed for namespace: %s, cause: %s",
                  m_namespace, ExceptionUtil.getDetailMessage(ex)));
      Tracer.logError(exception);
      throw exception;
    }
  }

  @Override
  public boolean hasContent() {
    return m_configProperties.get() != null && !m_configProperties.get().isEmpty();
  }

  @Override
  public ConfigFileFormat getConfigFileFormat() {
    return ConfigFileFormat.Properties;
  }

}
