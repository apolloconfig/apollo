package com.ctrip.framework.apollo.common.dto;


public class ItemInfoDTO extends BaseDTO{
    private String appId;
    private String appName;
    private String clusterName;
    private String namespaceName;
    private String status;
    private String key;
    private String value;

    public ItemInfoDTO() {
    }

    public ItemInfoDTO(String appId, String appName, String clusterName, String namespaceName,
                       String status, String key, String value) {
        this.appId = appId;
        this.appName = appName;
        this.clusterName = clusterName;
        this.namespaceName = namespaceName;
        this.status = status;
        this.key = key;
        this.value = value;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ItemInfoDTO{" +
                "appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", namespaceName='" + namespaceName + '\'' +
                ", status='" + status + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
