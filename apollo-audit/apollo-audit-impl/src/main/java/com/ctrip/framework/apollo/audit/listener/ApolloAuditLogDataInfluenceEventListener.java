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
package com.ctrip.framework.apollo.audit.listener;

import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.event.ApolloAuditLogDataInfluenceEvent;
import java.util.Collections;
import org.springframework.context.event.EventListener;

public class ApolloAuditLogDataInfluenceEventListener {

  private final ApolloAuditLogApi api;

  public ApolloAuditLogDataInfluenceEventListener(ApolloAuditLogApi api) {
    this.api = api;
  }

  @EventListener
  public void handleEvent(ApolloAuditLogDataInfluenceEvent event) {
    Object e = event.getEntity();
    api.appendDataInfluences(Collections.singletonList(e), event.getBeanDefinition());
  }
}
