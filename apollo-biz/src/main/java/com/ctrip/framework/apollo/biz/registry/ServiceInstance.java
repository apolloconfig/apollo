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
package com.ctrip.framework.apollo.biz.registry;

import java.net.URI;
import java.util.Map;

/**
 * @see org.springframework.cloud.client.ServiceInstance
 */
public interface ServiceInstance {

  /**
   * @return The service ID as registered.
   */
  String getServiceName();

  /**
   * get the uri of a service instance, for example:
   * <ul>
   *   <li><a href="http://localhost:8080/">http://localhost:8080/</a></li>
   *   <li><a href="http://10.240.12.34:8080/">http://10.240.12.34:8080/</a></li>
   *   <li><a href="http://47.56.23.34:8080/">http://47.56.23.34:8080/</a></li>
   * </ul>
   *
   * @return The service URI address.
   */
  URI getUri();

  /**
   * Tag a service instance for service discovery.
   * <p/>
   * so use cluster for service discovery.
   *
   * @return The cluster of the service instance.
   */
  String getCluster();

  /**
   * @return The key / value pair metadata associated with the service instance.
   * @see org.springframework.cloud.client.ServiceInstance#getMetadata()
   */
  Map<String, String> getMetadata();
}
