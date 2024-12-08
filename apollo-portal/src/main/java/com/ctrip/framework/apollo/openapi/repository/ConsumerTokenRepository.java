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
package com.ctrip.framework.apollo.openapi.repository;

import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConsumerTokenRepository extends PagingAndSortingRepository<ConsumerToken, Long> {
  /**
   * find consumer token by token
   *
   * @param token     the token
   * @param validDate the date when the token is valid
   */
  ConsumerToken findTopByTokenAndExpiresAfter(String token, Date validDate);

  ConsumerToken findByConsumerId(Long consumerId);

  List<ConsumerToken> findByConsumerIdIn(List<Long> consumerIds);

}
