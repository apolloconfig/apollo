package com.ctrip.framework.foundation.spi;

import com.ctrip.framework.foundation.spi.provider.Provider;

public interface ProviderManager {
  String getProperty(String name, String defaultValue);

  <T extends Provider> T provider(Class<T> clazz);
}
