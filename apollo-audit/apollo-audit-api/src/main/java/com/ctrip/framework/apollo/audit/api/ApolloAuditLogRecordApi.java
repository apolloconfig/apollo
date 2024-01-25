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
package com.ctrip.framework.apollo.audit.api;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import java.util.List;

/**
 * Mainly used to Record AuditLogs and DataInfluences.
 *
 * @author luke0125
 * @since 2.2.0
 */
public interface ApolloAuditLogRecordApi {

  /**
   * Append a new AuditLog by type and name.The operation's description would be default by "no
   * description".
   * <p></p>
   * Functionally aligned with annotations.
   * <p></p>
   * Need to close the audited scope manually!
   *
   * @param type operation's type
   * @param name operation's name
   * @return Returns an AuditScope needs to be closed when the audited operation ends.
   */
  AutoCloseable appendAuditLog(OpType type, String name);

  /**
   * Append a new AuditLog by type and name and description.
   * <p></p>
   * Functionally aligned with annotations.
   * <p></p>
   * Need to close the audited scope manually!
   *
   * @param type        operation's type
   * @param name        operation's name
   * @param description operation's description
   * @return Returns an AuditScope needs to be closed when the audited operation ends.
   */
  AutoCloseable appendAuditLog(OpType type, String name, String description);

  /**
   * Directly append a new DataInfluence by the attributes it should have.
   * <p></p>
   * Only when there is an active AuditScope in the context at this time can appending DataInfluence
   * be performed correctly. It will be considered to be caused by currently active operations.
   *
   * @param entityName        influenced entity's name (audit table name)
   * @param entityId          influenced entity's id (audit table id)
   * @param fieldName         influenced entity's field name (audit table field)
   * @param fieldCurrentValue influenced entity's field current value
   */
  void appendDataInfluence(String entityName, String entityId, String fieldName,
      String fieldCurrentValue);

  /**
   * Append DataInfluences by a list of entities needs to be audited, and their
   * audit-bean-definition.
   * <p></p>
   * Only when there is an active AuditScope in the context at this time can appending
   * DataInfluences be performed correctly. They will be considered to be caused by currently active
   * operations.
   *
   * @param entities       entities needs to be audited
   * @param beanDefinition entities' audit-bean-definition
   */
  void appendDataInfluences(List<Object> entities, Class<?> beanDefinition);

}
