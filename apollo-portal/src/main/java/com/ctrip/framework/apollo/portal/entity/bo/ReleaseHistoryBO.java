package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.common.entity.EntityPair;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReleaseHistoryBO {

  private long id;

  private String appId;

  private String clusterName;

  private String namespaceName;

  private String branchName;

  private String operator;

  private long releaseId;

  private String releaseTitle;

  private String releaseComment;

  private Date releaseTime;

  private String releaseTimeFormatted;

  private List<EntityPair<String>> configuration;

  private boolean isReleaseAbandoned;

  private long previousReleaseId;

  private int operation;

  private Map<String, Object> operationContext;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
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

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public long getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(long releaseId) {
    this.releaseId = releaseId;
  }

  public long getPreviousReleaseId() {
    return previousReleaseId;
  }

  public void setPreviousReleaseId(long previousReleaseId) {
    this.previousReleaseId = previousReleaseId;
  }

  public int getOperation() {
    return operation;
  }

  public void setOperation(int operation) {
    this.operation = operation;
  }

  public Map<String, Object> getOperationContext() {
    return operationContext;
  }

  public void setOperationContext(Map<String, Object> operationContext) {
    this.operationContext = operationContext;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getReleaseTitle() {
    return releaseTitle;
  }

  public void setReleaseTitle(String releaseTitle) {
    this.releaseTitle = releaseTitle;
  }

  public String getReleaseComment() {
    return releaseComment;
  }

  public void setReleaseComment(String releaseComment) {
    this.releaseComment = releaseComment;
  }

  public Date getReleaseTime() {
    return releaseTime;
  }

  public void setReleaseTime(Date releaseTime) {
    this.releaseTime = releaseTime;
  }

  public String getReleaseTimeFormatted() {
    return releaseTimeFormatted;
  }

  public void setReleaseTimeFormatted(String releaseTimeFormatted) {
    this.releaseTimeFormatted = releaseTimeFormatted;
  }

  public List<EntityPair<String>> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(
      List<EntityPair<String>> configuration) {
    this.configuration = configuration;
  }

  public boolean isReleaseAbandoned() {
    return isReleaseAbandoned;
  }

  public void setReleaseAbandoned(boolean releaseAbandoned) {
    isReleaseAbandoned = releaseAbandoned;
  }
}
