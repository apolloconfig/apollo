/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

public class ItemServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    @Sql(scripts = "/sql/item-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testSaveItem() {
        Item item = new Item();
        item.setNamespaceId(3);
        item.setKey("k3");
        item.setType(-1);
        item.setValue("v3");
        item.setComment("");
        item.setLineNum(3);

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
    @Sql(scripts = "/sql/item-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testUpdateItem() {
        Item item = new Item();
        item.setId(9901);
        item.setNamespaceId(1);
        item.setKey("k1");
        item.setType(2);
        item.setValue("v1-new");
        item.setComment("");
        item.setLineNum(1);

        Item dbItem = itemService.update(item);
        Assert.assertEquals(2, dbItem.getType());
        Assert.assertEquals("v1-new", dbItem.getValue());
    }

}
