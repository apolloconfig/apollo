/*
 * Copyright 2022 Apollo Authors
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
   *   <li>http://localhost:8080/</li>
   *   <li>http://10.240.12.34:8080/</li>
   *   <li>http://47.56.23.34:8080/</li>
   * </ul>
   * @return The service URI address.
   */
  URI getUri();

  /**
   * Tag a service instance for service discovery.
   * <p/>
   * It's a little hard to persist the key / value pair metadata to database,
   * so use a string 'label' instead of metadata.
   *
   * @see org.springframework.cloud.client.ServiceInstance#getMetadata()
   * @return The label of the service instance.
   */
  String getLabel();
}
