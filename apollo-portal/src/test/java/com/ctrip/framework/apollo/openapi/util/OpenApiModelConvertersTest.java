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
package com.ctrip.framework.apollo.openapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

public class OpenApiModelConvertersTest {

  @Test
  public void fromEnvClusterInfoShouldKeepPortalNavTreeFields() {
    ClusterDTO cluster = new ClusterDTO();
    cluster.setName("default");
    cluster.setAppId("someAppId");
    cluster.setComment("default cluster");

    EnvClusterInfo envClusterInfo = new EnvClusterInfo(Env.DEV);
    envClusterInfo.setClusters(Lists.newArrayList(cluster));

    OpenEnvClusterInfo result = OpenApiModelConverters.fromEnvClusterInfo(envClusterInfo);

    assertEquals("DEV", result.getEnv());
    assertEquals(1, result.getClusters().size());
    assertEquals("default", result.getClusters().get(0).getName());
    assertEquals("someAppId", result.getClusters().get(0).getAppId());
    assertEquals("default cluster", result.getClusters().get(0).getComment());
  }
}
