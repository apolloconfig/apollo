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
package com.ctrip.framework.apollo.core.schedule;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ExponentialSchedulePolicy implements SchedulePolicy {
  private final long delayTimeLowerBound;
  private final long delayTimeUpperBound;
  private long lastDelayTime;

  public ExponentialSchedulePolicy(long delayTimeLowerBound, long delayTimeUpperBound) {
    this.delayTimeLowerBound = delayTimeLowerBound;
    this.delayTimeUpperBound = delayTimeUpperBound;
  }

  @Override
  public long fail() {
    long delayTime = lastDelayTime;

    if (delayTime == 0) {
      delayTime = delayTimeLowerBound;
    } else {
      delayTime = Math.min(lastDelayTime << 1, delayTimeUpperBound);
    }

    lastDelayTime = delayTime;

    return delayTime;
  }

  @Override
  public void success() {
    lastDelayTime = 0;
  }
}
