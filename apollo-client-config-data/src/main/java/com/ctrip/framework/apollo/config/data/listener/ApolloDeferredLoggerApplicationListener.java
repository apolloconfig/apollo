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
package com.ctrip.framework.apollo.config.data.listener;

import com.ctrip.framework.apollo.core.utils.DeferredLogger;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloDeferredLoggerApplicationListener implements
    ApplicationListener<SpringApplicationEvent> {

  @Override
  public void onApplicationEvent(SpringApplicationEvent event) {
    if (event instanceof ApplicationContextInitializedEvent) {
      DeferredLogger.replayTo();
    }
    if (event instanceof ApplicationFailedEvent) {
      DeferredLogger.replayTo();
    }
  }
}
