/*
 * Copyright 2021 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.core;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientSystemConsts {

  /**
   * apollo client app id
   */
  public static final String APP_ID = "app.id";

  /**
   * apollo client app label
   */
  public static final String APOLLO_LABEL = "apollo.label";

  /**
   * apollo client app id environment variables
   */
  public static final String APP_ID_ENVIRONMENT_VARIABLES = "APP_ID";

  /**
   * apollo client app label environment variables
   */
  public static final String APOLLO_LABEL_ENVIRONMENT_VARIABLES = "APOLLO_LABEL";

  /**
   * cluster name
   */
  public static final String APOLLO_CLUSTER = ConfigConsts.APOLLO_CLUSTER_KEY;

  /**
   * cluster name environment variables
   */
  public static final String APOLLO_CLUSTER_ENVIRONMENT_VARIABLES = "APOLLO_CLUSTER";

  /**
   * local cache directory
   */
  public static final String APOLLO_CACHE_DIR = "apollo.cache-dir";

  /**
   * local cache directory
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_CACHE_DIR = "apollo.cacheDir";

  /**
   * local cache directory environment variables
   */
  public static final String APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES = "APOLLO_CACHE_DIR";

  /**
   * local cache directory environment variables
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES = "APOLLO_CACHEDIR";

  /**
   * apollo client access key
   */
  public static final String APOLLO_ACCESS_KEY_SECRET = "apollo.access-key.secret";

  /**
   * apollo client access key
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_ACCESS_KEY_SECRET = "apollo.accesskey.secret";

  /**
   * apollo client access key environment variables
   */
  public static final String APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES = "APOLLO_ACCESS_KEY_SECRET";

  /**
   * apollo client access key environment variables
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES = "APOLLO_ACCESSKEY_SECRET";

  /**
   * apollo meta server address
   */
  public static final String APOLLO_META = ConfigConsts.APOLLO_META_KEY;

  /**
   * apollo meta server address environment variables
   */
  public static final String APOLLO_META_ENVIRONMENT_VARIABLES = "APOLLO_META";

  /**
   * apollo config service address
   */
  public static final String APOLLO_CONFIG_SERVICE = "apollo.config-service";

  /**
   * apollo config service address
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_CONFIG_SERVICE = "apollo.configService";

  /**
   * apollo config service address environment variables
   */
  public static final String APOLLO_CONFIG_SERVICE_ENVIRONMENT_VARIABLES = "APOLLO_CONFIG_SERVICE";

  /**
   * apollo config service address environment variables
   */
  @Deprecated
  public static final String DEPRECATED_APOLLO_CONFIG_SERVICE_ENVIRONMENT_VARIABLES = "APOLLO_CONFIGSERVICE";

  /**
   * enable property order
   */
  public static final String APOLLO_PROPERTY_ORDER_ENABLE = "apollo.property.order.enable";

  /**
   * enable property order environment variables
   */
  public static final String APOLLO_PROPERTY_ORDER_ENABLE_ENVIRONMENT_VARIABLES = "APOLLO_PROPERTY_ORDER_ENABLE";

  /**
   * enable property names cache
   */
  public static final String APOLLO_PROPERTY_NAMES_CACHE_ENABLE = "apollo.property.names.cache.enable";

  /**
   * enable property names cache environment variables
   */
  public static final String APOLLO_PROPERTY_NAMES_CACHE_ENABLE_ENVIRONMENT_VARIABLES = "APOLLO_PROPERTY_NAMES_CACHE_ENABLE";
}
