package com.ctrip.framework.apollo.util.factory;

import java.util.Properties;

/**
 * Factory interface to construct Properties instances.
 *
 * @author songdragon@zts.io
 */
public interface PropertiesFactory {

  /**
   * Configuration to keep properties order as same as line order in .yml/.yaml/.properties file.
   */
  String APOLLO_PROPERTY_ORDER_ENABLE = "apollo.property.order.enable";

  /**
   * <pre>
   * Default implementation:
   * 1. if {@link APOLLO_PROPERTY_ORDER_ENABLE} is true return a new
   * instance of {@link com.ctrip.framework.apollo.util.OrderedProperties}.
   * 2. else return a new instance of {@link Properties}
   * </pre>
   *
   * @return
   */
  Properties getPropertiesInstance();
}
