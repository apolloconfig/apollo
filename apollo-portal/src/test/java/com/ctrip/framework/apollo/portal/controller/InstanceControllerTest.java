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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.entity.vo.Number;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class InstanceControllerTest {

  @Mock
  private InstanceService instanceService;

  @InjectMocks
  private InstanceController instanceController;

  @Captor
  private ArgumentCaptor<Set<Long>> releaseIdsCaptor;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(instanceController).build();
  }

  @Test
  public void shouldUseDefaultPageAndSizeForGetByRelease() throws Exception {
    PageDTO<InstanceDTO> page = new PageDTO<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
    when(instanceService.getByRelease(Env.DEV, 11L, 0, 20)).thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders.get("/envs/{env}/instances/by-release", "DEV")
        .param("releaseId", "11")).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(instanceService).getByRelease(Env.DEV, 11L, 0, 20);
  }

  @Test
  public void shouldParseReleaseIdsAndDeduplicateWhenQueryingInstances() {
    List<InstanceDTO> expected = Arrays.asList(new InstanceDTO(), new InstanceDTO());
    when(instanceService.getByReleasesNotIn(eq(Env.DEV), eq("SampleApp"), eq("default"),
        eq("application"), releaseIdsCaptor.capture())).thenReturn(expected);

    List<InstanceDTO> result = instanceController.getByReleasesNotIn("DEV", "SampleApp", "default",
        "application", "1,2,2,3");

    assertSame(expected, result);
    Set<Long> releaseIds = releaseIdsCaptor.getValue();
    assertEquals(3, releaseIds.size());
    org.junit.Assert.assertTrue(releaseIds.contains(1L));
    org.junit.Assert.assertTrue(releaseIds.contains(2L));
    org.junit.Assert.assertTrue(releaseIds.contains(3L));
  }

  @Test(expected = BadRequestException.class)
  public void shouldRejectEmptyReleaseIds() {
    instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "");
  }

  @Test
  public void shouldReturnInstanceCountByNamespace() {
    when(instanceService.getInstanceCountByNamespace("SampleApp", Env.DEV, "default", "application"))
        .thenReturn(8);

    ResponseEntity<Number> response =
        instanceController.getInstanceCountByNamespace("DEV", "SampleApp", "default", "application");

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(8, response.getBody().getNum());
  }
}
