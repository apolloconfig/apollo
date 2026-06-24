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
package com.ctrip.framework.apollo.biz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression test for the gray namespace item value length limit bug in ItemService.
 *
 * Bug location: ItemService.getGrayNamespaceItemValueLengthLimit(), line 243
 * Observation: getItemValueLengthLimit(grayNamespace) is called instead of
 *              getItemValueLengthLimit(parentNamespace).
 *
 * Expected behavior: When a gray namespace has a parent with a namespace-level value
 * length limit override, the parent's limit should be used.
 *
 * Actual (buggy) behavior: The gray namespace's own limit is used instead of the
 * parent's limit, making the parent namespace override useless.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceGrayNamespaceLimitTest {

  /**
   * Tests the core logic of getGrayNamespaceItemValueLengthLimit in isolation.
   *
   * This test directly exercises the method via reflection to avoid needing
   * a full Spring context.
   *
   * Scenario:
   * - Gray namespace (id=100) has no explicit override, gets default limit (20000)
   * - Parent namespace (id=200) has explicit override to 50000
   * - getGrayNamespaceItemValueLengthLimit(grayNamespace, 20000) should return 50000
   *
   * With the bug: returns 20000 (uses gray namespace's limit again, not parent's)
   * Without the bug: returns 50000 (uses parent namespace's limit)
   */
  @Test
  void getGrayNamespaceItemValueLengthLimit_shouldUseParentNamespaceLimit() throws Exception {
    // Arrange: Create gray and parent namespaces
    Namespace grayNamespace = new Namespace();
    grayNamespace.setId(100L);
    grayNamespace.setAppId("testApp");
    grayNamespace.setClusterName("20250101120000-abcdef1234567890");
    grayNamespace.setNamespaceName("application");

    Namespace parentNamespace = new Namespace();
    parentNamespace.setId(200L);
    parentNamespace.setAppId("testApp");
    parentNamespace.setClusterName("dev");
    parentNamespace.setNamespaceName("application");

    // Mock dependencies
    NamespaceService mockNamespaceService = org.mockito.Mockito.mock(NamespaceService.class);
    BizConfig mockBizConfig = org.mockito.Mockito.mock(BizConfig.class);

    // Setup BizConfig: default limit is 20000

    // Parent namespace (id=200) has override to 50000, gray namespace (id=100) does not
    Map<Long, Integer> namespaceOverride = new HashMap<>();
    namespaceOverride.put(200L, 50000);
    when(mockBizConfig.namespaceValueLengthLimitOverride()).thenReturn(namespaceOverride);

    // Setup namespaceService to return parent when queried
    when(mockNamespaceService.findParentNamespace(grayNamespace)).thenReturn(parentNamespace);

    // Create ItemService with mocked dependencies
    ItemService itemService = new ItemService(
        org.mockito.Mockito.mock(com.ctrip.framework.apollo.biz.repository.ItemRepository.class),
        mockNamespaceService, org.mockito.Mockito.mock(AuditService.class), mockBizConfig);

    // Act: Call the private method via reflection
    java.lang.reflect.Method method = ItemService.class
        .getDeclaredMethod("getGrayNamespaceItemValueLengthLimit", Namespace.class, int.class);
    method.setAccessible(true);
    int result = (int) method.invoke(itemService, grayNamespace, 20000);

    // Assert: The result should be 50000 (parent namespace's limit)
    // With the bug: result is 20000 (gray namespace's own limit used again)
    assertEquals(50000, result,
        "getGrayNamespaceItemValueLengthLimit should return the parent namespace's limit (50000), "
            + "not the gray namespace's own limit (20000). The parent namespace has id=200 with override 50000, "
            + "but the bug causes getItemValueLengthLimit(grayNamespace) to be called instead of "
            + "getItemValueLengthLimit(parentNamespace).");
  }

  /**
   * Tests that the full checkItemValueLength validation path uses the parent limit
   * for gray namespaces.
   *
   * This is an end-to-end test of the validation logic.
   */
  @Test
  void checkItemValueLength_forGrayNamespace_shouldAllowValueWithinParentLimit() throws Exception {
    // Arrange
    Namespace grayNamespace = new Namespace();
    grayNamespace.setId(100L);
    grayNamespace.setAppId("testApp");
    grayNamespace.setClusterName("20250101120000-abcdef1234567890");
    grayNamespace.setNamespaceName("application");

    Namespace parentNamespace = new Namespace();
    parentNamespace.setId(200L);
    parentNamespace.setAppId("testApp");
    parentNamespace.setClusterName("dev");
    parentNamespace.setNamespaceName("application");

    NamespaceService mockNamespaceService = org.mockito.Mockito.mock(NamespaceService.class);
    BizConfig mockBizConfig = org.mockito.Mockito.mock(BizConfig.class);

    when(mockBizConfig.itemValueLengthLimit()).thenReturn(20000);
    Map<Long, Integer> namespaceOverride = new HashMap<>();
    namespaceOverride.put(200L, 50000);
    when(mockBizConfig.namespaceValueLengthLimitOverride()).thenReturn(namespaceOverride);
    when(mockBizConfig.appIdValueLengthLimitOverride()).thenReturn(Collections.emptyMap());

    when(mockNamespaceService.findOne(100L)).thenReturn(grayNamespace);
    when(mockNamespaceService.findParentNamespace(grayNamespace)).thenReturn(parentNamespace);

    ItemService itemService = new ItemService(
        org.mockito.Mockito.mock(com.ctrip.framework.apollo.biz.repository.ItemRepository.class),
        mockNamespaceService, org.mockito.Mockito.mock(AuditService.class), mockBizConfig);

    // Create a value that exceeds gray namespace's limit (20000) but fits in parent's limit (50000)
    String value = "x".repeat(30000);

    // Act & Assert: checkItemValueLength should NOT throw because the parent's limit (50000)
    // applies
    java.lang.reflect.Method checkMethod =
        ItemService.class.getDeclaredMethod("checkItemValueLength", long.class, String.class);
    checkMethod.setAccessible(true);

    try {
      checkMethod.invoke(itemService, 100L, value);
      // If no exception, the parent limit was correctly used
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof BadRequestException) {
        // Bug is present: the gray namespace's own limit (20000) was used instead of parent's
        // (50000)
        throw new AssertionError(
            "Bug confirmed: checkItemValueLength used gray namespace limit instead of parent namespace limit. "
                + "Value length " + value.length() + " should be within parent limit 50000, "
                + "but gray namespace limit 20000 was enforced. Error: "
                + e.getCause().getMessage(),
            e);
      }
      throw new AssertionError("Unexpected exception: " + e.getCause(), e);
    }
  }
}
