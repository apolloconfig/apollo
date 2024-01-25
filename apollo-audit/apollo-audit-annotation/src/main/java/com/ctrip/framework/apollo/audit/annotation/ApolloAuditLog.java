/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark which method should be audited, add to controller or service's method.
 * <p></p>
 * Define the attributes of the operation for persisting and querying. When adding to controller's
 * methods, suggested that don't set name, and it will automatically be set to request's url.
 * <p></p>
 * Example usage:
 * <pre>
 * {@code
 * @ApolloAuditLog(type=OpType.CREATE,name="App.create")
 * public App create() {
 *   // ...
 * }
 * }
 * </pre>
 *
 * @author luke0125
 * @since 2.2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApolloAuditLog {

  /**
   * Define the type of operation.
   *
   * @return operation type
   */
  OpType type();

  /**
   * Define the name of operation. The requested URL will be taken by default if no specific name is
   * specified.
   *
   * @return operation name
   */
  String name() default "";

  /**
   * Define the description of operation. Default is "no description".
   *
   * @return operation description
   */
  String description() default "no description";
}

