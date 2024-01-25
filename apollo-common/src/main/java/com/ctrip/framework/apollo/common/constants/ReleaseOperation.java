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
package com.ctrip.framework.apollo.common.constants;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ReleaseOperation {
  int NORMAL_RELEASE = 0;
  int ROLLBACK = 1;
  int GRAY_RELEASE = 2;
  int APPLY_GRAY_RULES = 3;
  int GRAY_RELEASE_MERGE_TO_MASTER = 4;
  int MASTER_NORMAL_RELEASE_MERGE_TO_GRAY = 5;
  int MATER_ROLLBACK_MERGE_TO_GRAY = 6;
  int ABANDON_GRAY_RELEASE = 7;
  int GRAY_RELEASE_DELETED_AFTER_MERGE = 8;
}
