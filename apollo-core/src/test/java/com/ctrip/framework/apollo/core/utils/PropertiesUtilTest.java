package com.ctrip.framework.apollo.core.utils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

public class PropertiesUtilTest {


    @Test
    public void TestProperties() throws IOException {
        assertTrue("".equals(PropertiesUtil.toString(new Properties())));
        assertFalse(" ".equals(PropertiesUtil.toString(new Properties())));

        Properties properties = new Properties();
        properties.put("a","aaa");
        assertTrue("a=aaa\r\n".equals(PropertiesUtil.toString(properties)));

    }

    @Test
    public void TestFilterComment(){
        StringBuffer sb=new StringBuffer("#aaaaa\nbbb");
        PropertiesUtil.filterPropertiesComment(sb);
        assertTrue("bbb".equals(sb.toString()));
        assertFalse("#aaaaa\nbbb".equals(sb.toString()));
    }
}
