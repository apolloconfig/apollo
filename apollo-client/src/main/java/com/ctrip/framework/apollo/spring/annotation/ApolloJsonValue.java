/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to inject json property from Apollo, support the same format as Spring @Value.
 *
 * <p>Usage example:</p>
 * <pre class="code">
 * // Inject the json property value for type SomeObject.
 * // Suppose SomeObject has 2 properties, someString and someInt, then the possible config
 * // in Apollo is someJsonPropertyKey={"someString":"someValue", "someInt":10}.
 * &#064;ApolloJsonValue("${someJsonPropertyKey:someDefaultValue}")
 * private SomeObject someObject;
 * </pre>
 *
 * Create by zhangzheng on 2018/3/6
 *
 * @see org.springframework.beans.factory.annotation.Value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
public @interface ApolloJsonValue {

  /**
   * The actual value expression: e.g. "${someJsonPropertyKey:someDefaultValue}".
   */
  String value();
}
