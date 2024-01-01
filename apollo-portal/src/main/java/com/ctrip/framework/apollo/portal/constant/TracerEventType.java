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
package com.ctrip.framework.apollo.portal.constant;

public interface TracerEventType {

  String RELEASE_NAMESPACE = "Namespace.Release";

  String MODIFY_NAMESPACE_BY_TEXT = "Namespace.Modify.Text";

  String MODIFY_NAMESPACE = "Namespace.Modify";

  String SYNC_NAMESPACE = "Namespace.Sync";

  String CREATE_APP = "App.Create";

  String CREATE_CLUSTER = "Cluster.Create";

  String CREATE_ACCESS_KEY = "AccessKey.Create";

  String CREATE_NAMESPACE = "Namespace.Create";

  String API_RETRY = "API.Retry";

  String USER_ACCESS = "User.Access";

  String CREATE_GRAY_RELEASE = "GrayRelease.Create";

  String DELETE_GRAY_RELEASE = "GrayRelease.Delete";

  String MERGE_GRAY_RELEASE = "GrayRelease.Merge";

  String UPDATE_GRAY_RELEASE_RULE = "GrayReleaseRule.Update";
}
