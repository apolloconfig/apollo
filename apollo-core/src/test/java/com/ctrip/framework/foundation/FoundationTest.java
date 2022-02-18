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
package com.ctrip.framework.foundation;

import static org.junit.Assert.assertTrue;

import com.ctrip.framework.foundation.Foundation;
import com.ctrip.framework.foundation.internals.provider.DefaultApplicationProvider;
import com.ctrip.framework.foundation.internals.provider.DefaultServerProvider;
import org.junit.Assert;
import org.junit.Test;

public class FoundationTest {

   @Test
   public void testApp() {
      assertTrue(Foundation.app() instanceof DefaultApplicationProvider);
   }

   @Test
   public void testServer() {
      assertTrue(Foundation.server() instanceof DefaultServerProvider);
   }

   @Test
   public void testNet() {
      // 获取本机IP和HostName
      String hostAddress = Foundation.net().getHostAddress();
      String hostName = Foundation.net().getHostName();

      Assert.assertNotNull("No host address detected.", hostAddress);
      Assert.assertNotNull("No host name resolved.", hostName);
   }

}
