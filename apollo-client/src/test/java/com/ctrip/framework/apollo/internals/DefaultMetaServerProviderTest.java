package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.Env;
import org.junit.After;
import org.junit.Test;

public class DefaultMetaServerProviderTest {

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ConfigConsts.APOLLO_META_KEY);
  }

  @Test
  public void testWithSystemProperty() throws Exception {
    String someMetaAddress = "someMetaAddress";
    Env someEnv = Env.DEV;

    System.setProperty(ConfigConsts.APOLLO_META_KEY, " " + someMetaAddress + " ");

    DefaultMetaServerProvider defaultMetaServerProvider = new DefaultMetaServerProvider();

    assertEquals(someMetaAddress, defaultMetaServerProvider.getMetaServerAddress(someEnv));
  }

}
