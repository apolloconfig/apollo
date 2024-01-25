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
package com.ctrip.framework.apollo.biz.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Component
public class EntityManagerUtil extends EntityManagerFactoryAccessor {
  private static final Logger logger = LoggerFactory.getLogger(EntityManagerUtil.class);
  /**
   * close the entity manager.
   * Use it with caution! This is only intended for use with async request, which Spring won't
   * close the entity manager until the async request is finished.
   */
  public void closeEntityManager() {
    EntityManagerHolder emHolder = (EntityManagerHolder)
        TransactionSynchronizationManager.getResource(getEntityManagerFactory());
    if (emHolder == null) {
      return;
    }
    logger.debug("Closing JPA EntityManager in EntityManagerUtil");
    EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
  }
}
