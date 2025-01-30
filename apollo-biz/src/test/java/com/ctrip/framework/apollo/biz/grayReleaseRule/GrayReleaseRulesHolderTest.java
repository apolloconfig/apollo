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
package com.ctrip.framework.apollo.biz.grayReleaseRule;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.GrayReleaseRule;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.repository.GrayReleaseRuleRepository;
import com.ctrip.framework.apollo.common.constants.NamespaceBranchStatus;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.core.ConfigConsts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class GrayReleaseRulesHolderTest {
  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
  private GrayReleaseRulesHolder grayReleaseRulesHolder;
  @Mock
  private BizConfig bizConfig;
  @Mock
  private GrayReleaseRuleRepository grayReleaseRuleRepository;
  private static final Gson GSON = new Gson();
  private AtomicLong idCounter;

  @Before
  public void setUp() throws Exception {
    grayReleaseRulesHolder = spy(new GrayReleaseRulesHolder(grayReleaseRuleRepository, bizConfig));
    idCounter = new AtomicLong();
  }

  @Test
  public void testScanGrayReleaseRules() throws Exception {
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someNamespaceName = "someNamespaceName";
    String anotherNamespaceName = "anotherNamespaceName";

    Long someReleaseId = 1L;
    int activeBranchStatus = NamespaceBranchStatus.ACTIVE;

    String someClientAppId = "clientAppId1";
    String someClientIp = "1.1.1.1";
    String someClientLabel = "myLabel";
    String anotherClientAppId = "clientAppId2";
    String anotherClientIp = "2.2.2.2";
    String anotherClientLabel = "testLabel";

    GrayReleaseRule someRule = assembleGrayReleaseRule(someAppId, someClusterName,
        someNamespaceName, Lists.newArrayList(assembleRuleItem(someClientAppId, Sets.newHashSet
            (someClientIp), Sets.newHashSet(someClientLabel))), someReleaseId, activeBranchStatus);

    when(bizConfig.grayReleaseRuleScanInterval()).thenReturn(30);
    when(grayReleaseRuleRepository.findFirst500ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(Lists
        .newArrayList(someRule));

    //scan rules
    grayReleaseRulesHolder.afterPropertiesSet();

    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (someClientAppId, someClientIp, anotherClientLabel, someAppId, someClusterName, someNamespaceName));
    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (someClientAppId, anotherClientIp, someClientLabel, someAppId, someClusterName, someNamespaceName));
    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (someClientAppId.toUpperCase(), someClientIp, someClientLabel, someAppId.toUpperCase(), someClusterName, someNamespaceName.toUpperCase()));
    assertNull(grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule(someClientAppId,
        anotherClientIp, anotherClientLabel, someAppId, someClusterName, someNamespaceName));

    assertNull(grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule(anotherClientAppId,
        someClientIp, someClientLabel, someAppId, someClusterName, someNamespaceName));
    assertNull(grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule(anotherClientAppId,
        anotherClientIp, anotherClientLabel, someAppId, someClusterName, someNamespaceName));

    assertTrue(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, someClientIp, someClientLabel,
            someNamespaceName));
    assertTrue(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId.toUpperCase(), someClientIp,
            someClientLabel, someNamespaceName.toUpperCase()));
    assertTrue(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, anotherClientIp, someClientLabel,
            someNamespaceName));
    assertTrue(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId.toUpperCase(), anotherClientIp,
            someClientLabel, someNamespaceName.toUpperCase()));
    assertFalse(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, anotherClientIp,
            anotherClientLabel, someNamespaceName));
    assertFalse(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, someClientIp, someClientLabel,
            anotherNamespaceName));

    assertFalse(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, anotherClientIp,
        anotherClientLabel, someNamespaceName));
    assertFalse(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, anotherClientIp,
        anotherClientLabel, anotherNamespaceName));

    GrayReleaseRule anotherRule = assembleGrayReleaseRule(someAppId, someClusterName,
        someNamespaceName, Lists.newArrayList(assembleRuleItem(anotherClientAppId, Sets.newHashSet
            (anotherClientIp),Sets.newHashSet(anotherClientLabel))), someReleaseId, activeBranchStatus);

    when(grayReleaseRuleRepository.findByAppIdAndClusterNameAndNamespaceName(someAppId,
        someClusterName, someNamespaceName)).thenReturn(Lists.newArrayList(anotherRule));

    //send message
    grayReleaseRulesHolder.handleMessage(assembleReleaseMessage(someAppId, someClusterName,
        someNamespaceName), Topics.APOLLO_RELEASE_TOPIC);

    assertNull(grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (someClientAppId, someClientIp, someClientLabel, someAppId, someClusterName, someNamespaceName));
    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (anotherClientAppId, anotherClientIp, someClientLabel, someAppId, someClusterName, someNamespaceName));
    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (anotherClientAppId, someClientIp, anotherClientLabel, someAppId, someClusterName, someNamespaceName));
    assertEquals(someReleaseId, grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule
        (anotherClientAppId, anotherClientIp, anotherClientLabel, someAppId, someClusterName, someNamespaceName));

    assertFalse(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, someClientIp, someClientLabel, someNamespaceName));
    assertFalse(
        grayReleaseRulesHolder.hasGrayReleaseRule(someClientAppId, someClientIp, someClientLabel, anotherNamespaceName));

    assertTrue(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, anotherClientIp,
        anotherClientLabel, someNamespaceName));
    assertTrue(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, anotherClientIp,
        someClientLabel, someNamespaceName));
    assertTrue(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, someClientIp,
        anotherClientLabel, someNamespaceName));
    assertFalse(
        grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, someClientIp, someClientLabel,
            someNamespaceName));
    assertFalse(grayReleaseRulesHolder.hasGrayReleaseRule(anotherClientAppId, anotherClientIp,
        anotherClientLabel, anotherNamespaceName));
  }

  private GrayReleaseRule assembleGrayReleaseRule(String appId, String clusterName, String
      namespaceName, List<GrayReleaseRuleItemDTO> ruleItems, long releaseId, int branchStatus) {
    GrayReleaseRule rule = new GrayReleaseRule();
    rule.setId(idCounter.incrementAndGet());
    rule.setAppId(appId);
    rule.setClusterName(clusterName);
    rule.setNamespaceName(namespaceName);
    rule.setBranchName("someBranch");
    rule.setRules(GSON.toJson(ruleItems));
    rule.setReleaseId(releaseId);
    rule.setBranchStatus(branchStatus);

    return rule;
  }

  private GrayReleaseRuleItemDTO assembleRuleItem(String clientAppId, Set<String> clientIpList, Set<String> clientLabelList) {
    return new GrayReleaseRuleItemDTO(clientAppId, clientIpList, clientLabelList);
  }

  private ReleaseMessage assembleReleaseMessage(String appId, String clusterName, String
      namespaceName) {
    String message = STRING_JOINER.join(appId, clusterName, namespaceName);
    ReleaseMessage releaseMessage = new ReleaseMessage(message);

    return releaseMessage;
  }
}
