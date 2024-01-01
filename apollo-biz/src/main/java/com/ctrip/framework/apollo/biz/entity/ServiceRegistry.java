/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.biz.entity;

import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import java.time.LocalDateTime;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * use database as a registry instead of eureka, zookeeper, consul etc.
 * <p>
 * persist {@link ServiceInstance}
 */
@Entity
@Table(name = "`ServiceRegistry`")
public class ServiceRegistry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`Id`")
  private long id;

  @Column(name = "`ServiceName`", nullable = false)
  private String serviceName;

  /**
   * @see ServiceInstance#getUri()
   */
  @Column(name = "`Uri`", nullable = false)
  private String uri;

  /**
   * @see ServiceInstance#getCluster()
   */
  @Column(name = "`Cluster`", nullable = false)
  private String cluster;

  @Column(name = "`Metadata`", nullable = false)
  @Convert(converter = JpaMapFieldJsonConverter.class)
  private Map<String, String> metadata;

  @Column(name = "`DataChange_CreatedTime`", nullable = false)
  private LocalDateTime dataChangeCreatedTime;

  /**
   * modify by heartbeat
   */
  @Column(name = "`DataChange_LastTime`", nullable = false)
  private LocalDateTime dataChangeLastModifiedTime;

  @PrePersist
  protected void prePersist() {
    if (this.dataChangeCreatedTime == null) {
      dataChangeCreatedTime = LocalDateTime.now();
    }
    if (this.dataChangeLastModifiedTime == null) {
      dataChangeLastModifiedTime = dataChangeCreatedTime;
    }
  }

  @Override
  public String toString() {
    return "Registry{" +
        "id=" + id +
        ", serviceName='" + serviceName + '\'' +
        ", uri='" + uri + '\'' +
        ", cluster='" + cluster + '\'' +
        ", metadata='" + metadata + '\'' +
        ", dataChangeCreatedTime=" + dataChangeCreatedTime +
        ", dataChangeLastModifiedTime=" + dataChangeLastModifiedTime +
        '}';
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getDataChangeCreatedTime() {
    return dataChangeCreatedTime;
  }

  public void setDataChangeCreatedTime(LocalDateTime dataChangeCreatedTime) {
    this.dataChangeCreatedTime = dataChangeCreatedTime;
  }

  public LocalDateTime getDataChangeLastModifiedTime() {
    return dataChangeLastModifiedTime;
  }

  public void setDataChangeLastModifiedTime(LocalDateTime dataChangeLastModifiedTime) {
    this.dataChangeLastModifiedTime = dataChangeLastModifiedTime;
  }
}
