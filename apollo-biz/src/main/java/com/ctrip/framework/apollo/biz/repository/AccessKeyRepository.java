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
package com.ctrip.framework.apollo.biz.repository;


import com.ctrip.framework.apollo.biz.entity.AccessKey;
import java.util.Date;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AccessKeyRepository extends PagingAndSortingRepository<AccessKey, Long> {

  long countByAppId(String appId);

  AccessKey findOneByAppIdAndId(String appId, long id);

  List<AccessKey> findByAppId(String appId);

  List<AccessKey> findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(Date date);

  List<AccessKey> findByDataChangeLastModifiedTime(Date date);
}
