package com.ctrip.framework.foundation.spi.provider;

public interface Provider {
  /**
   * @return the current provider's type
   */
  Class<? extends Provider> getType();

  /**
   * Return the property value with the given name, or {@code defaultValue} if the name doesn't exist.
   *
   * @param name the property name
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  String getProperty(String name, String defaultValue);

  /**
   * Initialize the provider
   */
  void initialize();
}
