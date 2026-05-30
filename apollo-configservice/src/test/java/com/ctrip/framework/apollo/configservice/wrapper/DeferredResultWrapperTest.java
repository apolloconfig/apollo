/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.configservice.wrapper;

import static org.junit.Assert.assertSame;

import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import java.util.List;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class DeferredResultWrapperTest {

  @Test
  public void testSetResultReusesNotificationWhenNamespaceDoesNotNeedRestoration() {
    DeferredResultWrapper wrapper = new DeferredResultWrapper(1000);
    ApolloConfigNotification notification = new ApolloConfigNotification("application", 1);
    notification.addMessage("someKey", 1);

    wrapper.setResult(notification);

    ResponseEntity<?> response = (ResponseEntity<?>) wrapper.getResult().getResult();
    List<ApolloConfigNotification> notifications =
        (List<ApolloConfigNotification>) response.getBody();
    assertSame(notification, notifications.get(0));
  }
}
