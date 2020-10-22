package com.ctrip.framework.apollo.portal.util;

import org.apache.commons.lang.time.FastDateFormat;

import java.util.Calendar;
import java.util.Date;


public class RelativeDateFormat {
  private static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
  private static final long ONE_MINUTE = 60000L;
  private static final long ONE_HOUR = 3600000L;
  private static final long ONE_DAY = 86400000L;

  private static final String ONE_SECOND_AGO = " seconds ago";
  private static final String ONE_MINUTE_AGO = " minutes ago";
  private static final String ONE_HOUR_AGO = " hours ago";
  private static final String ONE_DAY_AGO = " days ago";
  private static final String ONE_MONTH_AGO = " months ago";

  public static String format(Date date) {
    if (date.after(new Date())) {
      return "now";
    }

    long delta = new Date().getTime() - date.getTime();
    if (delta < ONE_MINUTE) {
      long seconds = toSeconds(delta);
      return (seconds <= 0 ? 1 : seconds) + ONE_SECOND_AGO;
    }
    if (delta < 45L * ONE_MINUTE) {
      long minutes = toMinutes(delta);
      return (minutes <= 0 ? 1 : minutes) + ONE_MINUTE_AGO;
    }
    if (delta < 24L * ONE_HOUR) {
      long hours = toHours(delta);
      return (hours <= 0 ? 1 : hours) + ONE_HOUR_AGO;
    }

    Date lastDayBeginTime = getDateOffset(-1);
    if (date.after(lastDayBeginTime)) {
      return "yesterday";
    }
    Date lastTwoDaysBeginTime = getDateOffset(-2);
    if (date.after(lastTwoDaysBeginTime)) {
      return "the day before yesterday";
    }
    if (delta < 30L * ONE_DAY) {
      long days = toDays(delta);
      return (days <= 0 ? 1 : days) + ONE_DAY_AGO;
    }

    long months = toMonths(delta);
    if (months <= 3) {
      return (months <= 0 ? 1 : months) + ONE_MONTH_AGO;
    }
    return TIMESTAMP_FORMAT.format(date);
  }

  private static long toSeconds(long date) {
    return date / 1000L;
  }

  private static long toMinutes(long date) {
    return toSeconds(date) / 60L;
  }

  private static long toHours(long date) {
    return toMinutes(date) / 60L;
  }

  private static long toDays(long date) {
    return toHours(date) / 24L;
  }

  private static long toMonths(long date) {
    return toDays(date) / 30L;
  }

  public static Date getDateOffset(int offset) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.DATE, offset);

    return getDayBeginTime(calendar.getTime());
  }

  private static Date getDayBeginTime(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return new Date(calendar.getTime().getTime());
  }

}
