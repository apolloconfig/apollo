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
package com.ctrip.framework.apollo.portal.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeyValueUtilsTest {

    @Test
    public void testFilterWithKeyEndswith() {
        // map
        Map<String, String> map = new HashMap<>();
        map.put("abc.met", "none");
        map.put("abc_meta", "none");
        map.put("2bc.meta", "none");
        map.put("abc?met", "none");
        Map<String, String> afterFilter = KeyValueUtils.filterWithKeyIgnoreCaseEndsWith(map, "_meta");
        for(Map.Entry<String, String> entry : afterFilter.entrySet()) {
            String key = entry.getKey();
            assertTrue(key.endsWith("_meta"));
        }
    }

    @Test
    public void testRemoveKeySuffix() {
        Map<String, String> map = new HashMap<>();
        map.put("abc_meta", "none");
        map.put("234_meta", "none");
        map.put("888_meta", "none");
        Map<String, String> afterFilter = KeyValueUtils.removeKeySuffix(map, "_meta".length());
        for(Map.Entry<String, String> entry : afterFilter.entrySet()) {
            String key = entry.getKey();
            assertFalse(key.endsWith("_meta"));
            assertFalse(key.contains("_meta"));
        }
    }

}