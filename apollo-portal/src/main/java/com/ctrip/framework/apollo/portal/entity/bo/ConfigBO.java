package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.util.NamespaceBOUtils;

/**
 * a namespace represent.
 * @author wxq
 */
public class ConfigBO {

  private final Env env;

  private final String ownerName;

  private final String appId;

  private final String clusterName;

  private final String namespace;

  private final String configFileContent;

  private final ConfigFileFormat format;

  public ConfigBO(Env env, String ownerName, String appId, String clusterName,
      String namespace, String configFileContent, ConfigFileFormat format) {
    this.env = env;
    this.ownerName = ownerName;
    this.appId = appId;
    this.clusterName = clusterName;
    this.namespace = namespace;
    this.configFileContent = configFileContent;
    this.format = format;
  }

  public ConfigBO(Env env, String ownerName, String appId, String clusterName, NamespaceBO namespaceBO) {
    this(env, ownerName, appId, clusterName,
        namespaceBO.getBaseInfo().getNamespaceName(),
        NamespaceBOUtils.convert2configFileContent(namespaceBO),
        ConfigFileFormat.fromString(namespaceBO.getFormat())
    );
  }

  @Override
  public String toString() {
    return "ConfigBO{" +
        "env=" + env +
        ", ownerName='" + ownerName + '\'' +
        ", appId='" + appId + '\'' +
        ", clusterName='" + clusterName + '\'' +
        ", namespace='" + namespace + '\'' +
        ", configFileContent='" + configFileContent + '\'' +
        ", format=" + format +
        '}';
  }

  public Env getEnv() {
    return env;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getAppId() {
    return appId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getConfigFileContent() {
    return configFileContent;
  }

  public ConfigFileFormat getFormat() {
    return format;
  }
}
