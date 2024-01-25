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
package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import javax.persistence.Id;

@ApolloAuditLogDataInfluenceTable(tableName = "MockTableName")
public class MockDataInfluenceEntity {

  @Id
  private long id;

  @ApolloAuditLogDataInfluenceTableField(fieldName = "MarkedAttribute")
  private String markedAttribute;
  private String unMarkedAttribute;
  private boolean isDeleted;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getMarkedAttribute() {
    return markedAttribute;
  }

  public void setMarkedAttribute(String markedAttribute) {
    this.markedAttribute = markedAttribute;
  }

  public String getUnMarkedAttribute() {
    return unMarkedAttribute;
  }

  public void setUnMarkedAttribute(String unMarkedAttribute) {
    this.unMarkedAttribute = unMarkedAttribute;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }
}
