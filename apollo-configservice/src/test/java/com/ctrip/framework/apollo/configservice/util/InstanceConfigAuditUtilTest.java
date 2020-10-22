package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.service.InstanceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class InstanceConfigAuditUtilTest {
  private InstanceConfigAuditUtil instanceConfigAuditUtil;

  @Mock
  private InstanceService instanceService;
  private BlockingQueue<InstanceConfigAuditUtil.InstanceConfigAuditModel> audits;

  private String someAppId;
  private String someConfigClusterName;
  private String someClusterName;
  private String someDataCenter;
  private String someIp;
  private String someConfigAppId;
  private String someConfigNamespace;
  private String someReleaseKey;

  private InstanceConfigAuditUtil.InstanceConfigAuditModel someAuditModel;

  @Before
  public void setUp() throws Exception {
    instanceConfigAuditUtil = new InstanceConfigAuditUtil(instanceService);

    audits = (BlockingQueue<InstanceConfigAuditUtil.InstanceConfigAuditModel>)
        ReflectionTestUtils.getField(instanceConfigAuditUtil, "audits");

    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someDataCenter = "someDataCenter";
    someIp = "someIp";
    someConfigAppId = "someConfigAppId";
    someConfigClusterName = "someConfigClusterName";
    someConfigNamespace = "someConfigNamespace";
    someReleaseKey = "someReleaseKey";

    someAuditModel = new InstanceConfigAuditUtil.InstanceConfigAuditModel(someAppId,
        someClusterName, someDataCenter, someIp, someConfigAppId, someConfigClusterName,
        someConfigNamespace, someReleaseKey);
  }

  @Test
  public void testAudit() throws Exception {
    boolean result = instanceConfigAuditUtil.audit(someAppId, someClusterName, someDataCenter,
        someIp, someConfigAppId, someConfigClusterName, someConfigNamespace, someReleaseKey);

    InstanceConfigAuditUtil.InstanceConfigAuditModel audit = audits.poll();

    assertTrue(result);
    assertTrue(Objects.equals(someAuditModel, audit));
  }

  @Test
  public void testDoAudit() throws Exception {
    long someInstanceId = 1;
    Instance someInstance = mock(Instance.class);

    when(someInstance.getId()).thenReturn(someInstanceId);
    when(instanceService.createInstance(any(Instance.class))).thenReturn(someInstance);

    instanceConfigAuditUtil.doAudit(someAuditModel);

    verify(instanceService, times(1)).findInstance(someAppId, someClusterName, someDataCenter,
        someIp);
    verify(instanceService, times(1)).createInstance(any(Instance.class));
    verify(instanceService, times(1)).findInstanceConfig(someInstanceId, someConfigAppId,
        someConfigNamespace);
    verify(instanceService, times(1)).createInstanceConfig(any(InstanceConfig.class));
  }


}
