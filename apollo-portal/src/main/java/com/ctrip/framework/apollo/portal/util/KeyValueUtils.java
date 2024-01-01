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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * some tools for manipulate key in map and properties
 * @author wxq
 */
public class KeyValueUtils {

    /**
     * make a filter on properties.
     * and convert properties to a map
     * the suffix match is case insensitive
     * @param properties
     * @param suffix suffix in a key
     * @return a map which key is ends with suffix
     */
    public static Map<String, String> filterWithKeyIgnoreCaseEndsWith(Properties properties, String suffix) {
        // use O(n log(n)) algorithm
        Map<String, String> keyValues = new HashMap<>();
        for(String propertyName : properties.stringPropertyNames()) {
            keyValues.put(propertyName, properties.getProperty(propertyName));
        }
        return filterWithKeyIgnoreCaseEndsWith(keyValues, suffix);
    }

    /**
     * make a filter on map's key,
     * keep the k-v which key ends with suffix given
     * the suffix match is case insensitive
     * @param keyValues
     * @param suffix suffix in a key
     * @return a map which key is ends with suffix
     */
    public static Map<String, String> filterWithKeyIgnoreCaseEndsWith(Map<String, String> keyValues, String suffix) {
        // use O(n) algorithm
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // let key and suffix both to upper,
            // so the suffix match doesn't care about the character is upper or lower
            if(key.toUpperCase().endsWith(suffix.toUpperCase())) {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * remove key's suffix in a map
     * suppose that all keys's length not smaller than suffixLength,
     * if not satisfied, a terrible runtime exception will occur
     * @param keyValues
     * @param suffixLength suffix string's length
     * @return
     */
    public static Map<String, String> removeKeySuffix(Map<String, String> keyValues, int suffixLength) {
        // use O(n) algorithm
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String newKey = key.substring(0, key.length() - suffixLength);
            map.put(newKey, value);
        }
        return map;
    }

}
