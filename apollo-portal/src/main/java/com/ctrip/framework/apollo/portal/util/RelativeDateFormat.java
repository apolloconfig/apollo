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
package com.ctrip.framework.apollo.portal.util;

import org.apache.commons.lang3.time.FastDateFormat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class RelativeDateFormat {
  private static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

  private static final String ONE_SECOND_AGO = " seconds ago";
  private static final String ONE_MINUTE_AGO = " minutes ago";
  private static final String ONE_HOUR_AGO = " hours ago";
  private static final String ONE_DAY_AGO = " days ago";
  private static final String ONE_MONTH_AGO = " months ago";

  public static String format(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        Duration duration = Duration.between(localDateTime, LocalDateTime.now());
        if (duration.toMillis() <= 0L) {
            return "now";
        }
        if (duration.getSeconds() <= 60L) {
            return (duration.getSeconds() <= 0 ? 1 : duration.getSeconds()) + ONE_SECOND_AGO;
        }
        if (duration.toMinutes() < 45L) {
            return (duration.toMinutes() <= 0 ? 1 : duration.toMinutes()) + ONE_MINUTE_AGO;
        }
        if (duration.toHours() < 24L) {
            return (duration.toHours() <= 0 ? 1 : duration.toHours()) + ONE_HOUR_AGO;
        }
        if (localDateTime.isAfter(LocalDateTime.now().minusDays(1))) {
            return "yesterday";
        }
        if (localDateTime.isAfter(LocalDateTime.now().minusDays(2))) {
            return "the day before yesterday";
        }
        if (duration.toDays() < 30L) {
            return (duration.toDays() <= 0 ? 1 : duration.toDays()) + ONE_DAY_AGO;
        }
        if (duration.toDays() / 30 <= 3L) {
            return duration.toDays() / 30 + ONE_MONTH_AGO;
        }
        return TIMESTAMP_FORMAT.format(date);
    }
}
