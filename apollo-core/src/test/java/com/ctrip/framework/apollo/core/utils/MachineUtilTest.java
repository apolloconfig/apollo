package com.ctrip.framework.apollo.core.utils;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MachineUtilTest {

  @InjectMocks
  private MachineUtil machineUtil;

  @Test
  public void testGetMachineIdentifier() {
    assertNotNull(MachineUtil.getMachineIdentifier());
  }
}
