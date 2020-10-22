package com.ctrip.framework.foundation.spi.provider;

/**
 * Provider for network related properties
 */
public interface NetworkProvider extends Provider {
  /**
   * @return the host address, i.e. ip
   */
  String getHostAddress();

  /**
   * @return the host name
   */
  String getHostName();
}
