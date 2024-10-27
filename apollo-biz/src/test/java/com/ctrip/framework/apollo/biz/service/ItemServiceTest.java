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
package com.ctrip.framework.apollo.biz.service;

import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.repository.ItemRepository;
import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

public class ItemServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private AuditService auditService;

    @Mock
    private BizConfig bizConfig;

    private ItemService itemService2;

    @Before
    public void setUp() throws Exception {
        itemService2 = new ItemService(itemRepository, namespaceService, auditService, bizConfig);
    }

    @Test
    @Sql(scripts = {"/sql/namespace-test.sql","/sql/item-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testSaveItem() {
        Item item = createItem(1L, "k3", "v3", -1);
        try {
            itemService.save(item);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException);
        }

        item.setType(0);
        Item dbItem = itemService.save(item);
        Assert.assertEquals(0, dbItem.getType());
    }

    @Test
    @Sql(scripts = {"/sql/namespace-test.sql", "/sql/item-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testSaveItemWithNamespaceValueLengthLimitOverride() {

        long namespaceId = 1L;
        String itemValue = "test-demo";

        Map<Long, Integer> namespaceValueLengthOverride = new HashMap<>();
        namespaceValueLengthOverride.put(namespaceId, itemValue.length() - 1);
        when(bizConfig.namespaceValueLengthLimitOverride()).thenReturn(namespaceValueLengthOverride);
        when(bizConfig.itemKeyLengthLimit()).thenReturn(100);

        Item item = createItem(namespaceId, "k3", itemValue, 2);
        try {
            itemService2.save(item);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException && e.getMessage().contains("value too long"));
        }
    }

    @Test
    @Sql(scripts = {"/sql/namespace-test.sql", "/sql/item-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testSaveItemWithAppIdValueLengthLimitOverride() {

        String appId = "testApp";
        long namespaceId = 1L;
        String itemValue = "test-demo";

        Map<String, Integer> appIdValueLengthOverride = new HashMap<>();
        appIdValueLengthOverride.put(appId, itemValue.length() - 1);
        when(bizConfig.appIdValueLengthLimitOverride()).thenReturn(appIdValueLengthOverride);
        when(bizConfig.itemKeyLengthLimit()).thenReturn(100);

        Item item = createItem(namespaceId, "k3", itemValue, 2);
        try {
            itemService2.save(item);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException && e.getMessage().contains("value too long"));
        }
    }

    @Test
    @Sql(scripts = {"/sql/namespace-test.sql","/sql/item-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testUpdateItem() {
        Item item = createItem(1, "k1", "v1-new", 2);
        item.setId(9901);
        item.setLineNum(1);

        Item dbItem = itemService.update(item);
        Assert.assertEquals(2, dbItem.getType());
        Assert.assertEquals("v1-new", dbItem.getValue());
    }

    @Test
    @Sql(scripts = {"/sql/namespace-test.sql","/sql/item-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testSearchItem() {
        ItemInfoDTO itemInfoDTO = new ItemInfoDTO();
        itemInfoDTO.setAppId("testApp");
        itemInfoDTO.setClusterName("default");
        itemInfoDTO.setNamespaceName("application");
        itemInfoDTO.setKey("k1");
        itemInfoDTO.setValue("v1");

        String itemKey = "k1";
        String itemValue = "v1";
        Page<ItemInfoDTO> ExpectedItemInfoDTOSByKeyAndValue = itemService.getItemInfoBySearch(itemKey, itemValue, PageRequest.of(0,200));
        Page<ItemInfoDTO> ExpectedItemInfoDTOSByKey = itemService.getItemInfoBySearch(itemKey,"", PageRequest.of(0,200));
        Page<ItemInfoDTO> ExpectedItemInfoDTOSByValue = itemService.getItemInfoBySearch("", itemValue, PageRequest.of(0,200));
        Assert.assertEquals(itemInfoDTO.toString(), ExpectedItemInfoDTOSByKeyAndValue.getContent().get(0).toString());
        Assert.assertEquals(itemInfoDTO.toString(), ExpectedItemInfoDTOSByKey.getContent().get(0).toString());
        Assert.assertEquals(itemInfoDTO.toString(), ExpectedItemInfoDTOSByValue.getContent().get(0).toString());

    }

    private Item createItem(long namespaceId, String key, String value, int type) {
        Item item = new Item();
        item.setNamespaceId(namespaceId);
        item.setKey(key);
        item.setValue(value);
        item.setType(type);
        item.setComment("");
        item.setLineNum(3);
        return item;
    }

}
