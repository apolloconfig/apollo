package com.ctrip.framework.apollo.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ctrip.framework.apollo.core.ConfigConsts;

/**
 * Use this annotation to inject Apollo Config Instance.
 *
 * <p>Usage example:</p>
 * <pre class="code">
 * //Inject the config for "someNamespace"
 * &#064;ApolloConfig("someNamespace")
 * private Config config;
 * </pre>
 *
 * <p>Usage example with placeholder:</p>
 * <pre class="code">
 * // The namespace could also be specified as a placeholder, e.g. ${redis.namespace:xxx},
 * // which will use the value of the key "redis.namespace" or "xxx" if this key is not configured.
 * &#064;ApolloConfig("${redis.namespace:xxx}")
 * private Config config;
 * </pre>
 *
 *
 * @author Jason Song(song_s@ctrip.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ApolloConfig {
  /**
   * Apollo namespace for the config, if not specified then default to application
   */
  String value() default ConfigConsts.NAMESPACE_APPLICATION;
}
