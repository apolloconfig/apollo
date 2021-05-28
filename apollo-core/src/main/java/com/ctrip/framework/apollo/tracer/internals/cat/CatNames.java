/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.tracer.internals.cat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface CatNames {
  String CAT_CLASS = "com.dianping.cat.Cat";
  String LOG_ERROR_METHOD = "logError";
  String LOG_EVENT_METHOD = "logEvent";
  String NEW_TRANSACTION_METHOD = "newTransaction";

  String CAT_TRANSACTION_CLASS = "com.dianping.cat.message.Transaction";
  String SET_STATUS_METHOD = "setStatus";
  String ADD_DATA_METHOD = "addData";
  String COMPLETE_METHOD = "complete";
}
