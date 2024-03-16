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
package com.ctrip.framework.apollo.common.controller;

import com.ctrip.framework.apollo.common.constants.ApolloServer;
import com.ctrip.framework.foundation.Foundation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/apollo")
public class ApolloInfoController {

  @RequestMapping("net")
  public String getNet() {
    return Foundation.net().toString();
  }

  @RequestMapping("server")
  public String getServer() {
    return Foundation.server().toString();
  }

  @RequestMapping("version")
  public String getVersion() {
    return ApolloServer.VERSION;
  }
}
