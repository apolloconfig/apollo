/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.core.utils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @author Wu Mingkan(Dalian University of Technology)
 * @since 2021/11/23
 *
 */
public class PropertiesUtilTest {


    @Test
    public void TestProperties() throws IOException {
        assertTrue("".equals(PropertiesUtil.toString(new Properties())));
        assertFalse(" ".equals(PropertiesUtil.toString(new Properties())));

        Properties properties = new Properties();
        properties.put("a","aaa");
        assertTrue("a=aaa\r\n".equals(PropertiesUtil.toString(properties))
                || "a=aaa\n".equals(PropertiesUtil.toString(properties)));

    }

    @Test
    public void TestFilterComment(){
        StringBuffer sb=new StringBuffer("#aaaaa\nbbb");
        PropertiesUtil.filterPropertiesComment(sb);
        assertTrue("bbb".equals(sb.toString()));
        assertFalse("#aaaaa\nbbb".equals(sb.toString()));
    }
}
