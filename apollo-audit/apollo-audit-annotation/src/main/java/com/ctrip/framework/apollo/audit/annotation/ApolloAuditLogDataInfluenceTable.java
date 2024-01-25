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
 * Mainly used in class definitions, indicates the name of the corresponding audit data table of
 * this class.
 * <p></p>
 * It could also be used on method parameters to express the table name of the class which this
 * parameter belongs to.
 * <p></p>
 * Example usage:
 * <pre>
 * {@code
 * CASE 1:
 * @ApolloAuditLogDataInfluenceTable(tableName="App")
 * public class App {
 *   // ...
 * }
 * CASE 2:
 * @ApolloAuditLog(type=OpType.DELETE,name="AppNamespace.batchDeleteByAppId")
 * public AppNamespace batchDeleteByAppId(
 *   @ApolloAuditLogDataInfluence
 *   @ApolloAuditLogDataInfluenceTable(tableName="AppNamespace") String appId) {
 *   // ...
 * }
 * }
 * </pre>
 *
 * @author luke0125
 * @since 2.2.0
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApolloAuditLogDataInfluenceTable {

  /**
   * Define the table name(entity name) of audited entity.
   *
   * @return table name
   */
  String tableName();

}
