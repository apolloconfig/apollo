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
package com.ctrip.framework.apollo.core.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.BaseIntegrationTest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.Test;

/**
 * Created by gl49 on 2018/6/8.
 */
public class NetUtilTest extends BaseIntegrationTest {

  @Test
  public void testPingUrlWithStatusCode200() throws Exception {
    String someResponse = "some response";
    ContextHandler handler = mockServerHandler(HttpServletResponse.SC_OK, someResponse);
    startServerWithHandlers(handler);

    assertTrue(NetUtil.pingUrl("http://localhost:" + PORT));
  }

  @Test
  public void testPingUrlWithStatusCode404() throws Exception {
    String someResponse = "some response";
    startServerWithHandlers(mockServerHandler(HttpServletResponse.SC_NOT_FOUND, someResponse));

    assertFalse(NetUtil.pingUrl("http://localhost:" + PORT));
  }

  @Test
  public void testPingUrlWithServerNotStarted() throws Exception {
    assertFalse(NetUtil.pingUrl("http://localhost:" + PORT));
  }
}
