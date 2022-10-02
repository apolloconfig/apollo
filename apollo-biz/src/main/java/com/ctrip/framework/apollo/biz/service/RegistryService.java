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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.Registry;
import com.ctrip.framework.apollo.biz.repository.RegistryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RegistryService {

  private final RegistryRepository repository;

  public RegistryService(RegistryRepository repository) {
    this.repository = repository;
  }

  public Registry saveIfNotExistByServiceNameAndUri(Registry registry) {
    Registry registrySaved = this.repository.findByServiceNameAndUri(registry.getServiceName(), registry.getUri());
    if (null == registrySaved) {
      registrySaved = registry;
    } else {
      // update
      registrySaved.setLabel(registry.getLabel());
      registrySaved.setDataChangeLastModifiedTime(LocalDateTime.now());
    }
    return this.repository.save(registrySaved);
  }

  public void delete(Registry registry) {
    this.repository.deleteByServiceNameAndUri(
        registry.getServiceName(), registry.getUri()
    );
  }

  public List<Registry> findByServiceName(String serviceName) {
    return this.repository.findByServiceName(serviceName);
  }

  public LocalDateTime getTimeBeforeSeconds(long seconds) {
    return this.repository.currentTimestamp().minusSeconds(seconds);
  }

  public List<Registry> deleteTimeBefore(Duration duration) {
    LocalDateTime time = this.repository.currentTimestamp().minus(duration);
    return this.repository.deleteByDataChangeLastModifiedTimeLessThan(time);
  }
}
