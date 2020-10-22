package com.ctrip.framework.foundation.spi.provider;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provider for server related properties
 */
public interface ServerProvider extends Provider {
  /**
   * @return current environment or {@code null} if not set
   */
  String getEnvType();

  /**
   * @return whether current environment is set or not
   */
  boolean isEnvTypeSet();

  /**
   * @return current data center or {@code null} if not set
   */
  String getDataCenter();

  /**
   * @return whether data center is set or not
   */
  boolean isDataCenterSet();

  /**
   * Initialize server provider with the specified input stream
   *
   * @throws IOException
   */
  void initialize(InputStream in) throws IOException;
}
