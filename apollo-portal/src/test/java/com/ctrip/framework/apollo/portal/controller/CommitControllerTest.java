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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import org.junit.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

/**
 * Created by kezhenxu at 2019/1/14 12:49.
 *
 * @author kezhenxu (kezhenxu at lizhi dot fm)
 */
public class CommitControllerTest extends AbstractIntegrationTest {

  @Test
  public void shouldFailWhenPageOrSiseIsNegative() {
    try {
      restTemplate.getForEntity(
          url("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/commits?page=-1"),
          List.class, "1", "env", "cl", "ns"
      );
      fail("should throw");
    } catch (final HttpClientErrorException e) {
     assertThat(
          new String(e.getResponseBodyAsByteArray()), containsString("page should be positive or 0")
      );
    }
    try {
      restTemplate.getForEntity(
          url("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/commits?size=0"),
          List.class, "1", "env", "cl", "ns"
      );
      fail("should throw");
    } catch (final HttpClientErrorException e) {
      assertThat(
          new String(e.getResponseBodyAsByteArray()), containsString("size should be positive number")
      );
    }
  }
}
