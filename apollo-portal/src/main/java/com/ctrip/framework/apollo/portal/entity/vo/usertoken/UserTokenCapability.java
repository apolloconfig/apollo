/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.entity.vo.usertoken;

import java.util.List;

public class UserTokenCapability {

  private List<String> operations;
  private int defaultExpireDays;
  private int maxExpireDays;

  public List<String> getOperations() {
    return operations;
  }

  public void setOperations(List<String> operations) {
    this.operations = operations;
  }

  public int getDefaultExpireDays() {
    return defaultExpireDays;
  }

  public void setDefaultExpireDays(int defaultExpireDays) {
    this.defaultExpireDays = defaultExpireDays;
  }

  public int getMaxExpireDays() {
    return maxExpireDays;
  }

  public void setMaxExpireDays(int maxExpireDays) {
    this.maxExpireDays = maxExpireDays;
  }
}
