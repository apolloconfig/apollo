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
package com.ctrip.framework.apollo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.Test;

public class GrayReleaseRuleItemTransformerTest {

  @Test
  public void testBatchTransformFromJSONShouldTreatNullAsEmptyRules() {
    Set<GrayReleaseRuleItemDTO> ruleItems =
        GrayReleaseRuleItemTransformer.batchTransformFromJSON(null);

    assertNotNull(ruleItems);
    assertTrue(ruleItems.isEmpty());
  }

  @Test
  public void testBatchTransformFromJSONShouldTreatEmptyArrayAsEmptyRules() {
    Set<GrayReleaseRuleItemDTO> ruleItems =
        GrayReleaseRuleItemTransformer.batchTransformFromJSON("[]");

    assertNotNull(ruleItems);
    assertTrue(ruleItems.isEmpty());
    assertEquals(GrayReleaseRuleItemTransformer.batchTransformFromJSON(null), ruleItems);
  }

  @Test
  public void testBatchTransformFromJSONShouldKeepRuleItems() {
    GrayReleaseRuleItemDTO ruleItem =
        new GrayReleaseRuleItemDTO("clientAppId", Sets.newHashSet("1.1.1.1"), Sets.newHashSet());

    Set<GrayReleaseRuleItemDTO> ruleItems = GrayReleaseRuleItemTransformer.batchTransformFromJSON(
        GrayReleaseRuleItemTransformer.batchTransformToJSON(Sets.newHashSet(ruleItem)));

    assertEquals(1, ruleItems.size());
    GrayReleaseRuleItemDTO parsed = ruleItems.iterator().next();
    assertEquals("clientAppId", parsed.getClientAppId());
    assertTrue(parsed.getClientIpList().contains("1.1.1.1"));
  }
}
