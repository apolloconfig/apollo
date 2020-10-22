package com.ctrip.framework.foundation.spi.provider;

import java.io.InputStream;

/**
 * Provider for application related properties
 */
public interface ApplicationProvider extends Provider {
  /**
   * @return the application's app id
   */
  String getAppId();

  /**
   * @return the application's access key secret
   */
  String getAccessKeySecret();

  /**
   * @return whether the application's app id is set or not
   */
  boolean isAppIdSet();

  /**
   * Initialize the application provider with the specified input stream
   */
  void initialize(InputStream in);
}
