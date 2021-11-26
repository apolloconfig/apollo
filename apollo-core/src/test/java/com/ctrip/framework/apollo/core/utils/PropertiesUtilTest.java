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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


import java.io.IOException;
import java.util.Properties;

/**
 * @author Wu Mingkan(Dalian University of Technology)
 * @since 2021/11/23
 */
public class PropertiesUtilTest {


  @Test
  public void testToString() throws IOException {

    assertEquals("", PropertiesUtil.toString(new Properties()));
    assertNotEquals(" ", PropertiesUtil.toString(new Properties()));

    Properties properties = new Properties();
    properties.put("a", "aaa");
    assertEquals("a=aaa" + System.lineSeparator(), PropertiesUtil.toString(properties));
  }

  @Test
  public void testFilterPropertiesComment() {

    StringBuffer sb1 = new StringBuffer(System.lineSeparator());
    PropertiesUtil.filterPropertiesComment(sb1);
    boolean equals = "".equals(sb1.toString());
    assertEquals(false, equals);

    StringBuffer sb2 = new StringBuffer("#aaa" + System.lineSeparator());
    PropertiesUtil.filterPropertiesComment(sb2);
    assertEquals("", sb2.toString());

    StringBuffer sb3 = new StringBuffer("#aaaaa" + System.lineSeparator() + "bbb");
    PropertiesUtil.filterPropertiesComment(sb3);
    assertEquals("bbb", sb3.toString());
    assertNotEquals("#aaaaa" + System.lineSeparator() + "bbb", sb3.toString());
  }
}
