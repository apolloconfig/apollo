/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.foundation.internals;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    private final String actualOsName = System.getProperty("os.name");

    @Test
    public void isBlankTrueGivenNull() {
        assertTrue(Utils.isBlank(null));
    }

    @Test
    public void isBlankTrueGivenEmpty() {
        assertTrue(Utils.isBlank(""));
    }

    @Test
    public void isBlankTrueGivenWhitespace() {
        assertTrue(Utils.isBlank("   "));
    }

    @Test
    public void isBlankFalseGivenLoremIpsum() {
        assertFalse(Utils.isBlank("Lorem Ipsum"));
    }

    @Test
    public void isBlankFalseGivenWhitespacePadded() {
        assertFalse(Utils.isBlank("   Lorem Ipsum   "));
    }

    @Test
    public void isOsWindowsTrueGivenWindows10() {
        if (actualOsName.startsWith("Windows")) {
            assertTrue(Utils.isOSWindows());
        } else {
            assertFalse(Utils.isOSWindows());
        }
    }

}
