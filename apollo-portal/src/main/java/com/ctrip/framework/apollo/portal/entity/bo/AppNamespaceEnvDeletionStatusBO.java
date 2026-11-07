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
package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.portal.environment.Env;

/**
 * The status of an AppNamespace in one environment, as a deletion check finds it.
 */
public class AppNamespaceEnvDeletionStatusBO {

  public enum Status {
    /** The environment still has the AppNamespace. */
    REMAINING,
    /** The environment does not have the AppNamespace. */
    ABSENT,
    /** The environment could not be checked. */
    UNREACHABLE,
    /** The health check marks the environment down, so it was not checked. */
    SKIPPED
  }

  private final Env env;

  private final Status status;

  private final String message;

  public AppNamespaceEnvDeletionStatusBO(Env env, Status status, String message) {
    this.env = env;
    this.status = status;
    this.message = message;
  }

  public Env getEnv() {
    return env;
  }

  public Status getStatus() {
    return status;
  }

  /**
   * @return the error of the check when the environment is unreachable, otherwise null
   */
  public String getMessage() {
    return message;
  }
}
