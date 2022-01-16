/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.util.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parsers {
  public static DateParser forDate() {
    return DateParser.INSTANCE;
  }

  public static DurationParser forDuration() {
    return DurationParser.INSTANCE;
  }

  public enum DateParser {
    INSTANCE;

    private static final String LONG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String MEDIUM_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Will try to parse the date with Locale.US and formats as follows:
     * yyyy-MM-dd HH:mm:ss.SSS, yyyy-MM-dd HH:mm:ss and yyyy-MM-dd
     *
     * @param text the text to parse
     * @return the parsed date
     * @throws ParserException if the text cannot be parsed
     */
    public Date parse(String text) throws ParserException {
      text = text.trim();
      int length = text.length();

      if (length == LONG_DATE_FORMAT.length()) {
        return parse(text, LONG_DATE_FORMAT);
      }

      if (length == MEDIUM_DATE_FORMAT.length()) {
        return parse(text, MEDIUM_DATE_FORMAT);
      }

      return parse(text, SHORT_DATE_FORMAT);
    }

    /**
     * Parse the text with the format specified and Locale.US
     *
     * @param text   the text to parse
     * @param format the date format, see {@link java.text.SimpleDateFormat} for more information
     * @return the parsed date
     * @throws ParserException if the text cannot be parsed
     */
    public Date parse(String text, String format) throws ParserException {
      return parse(text, format, Locale.US);
    }

    /**
     * Parse the text with the format and locale specified
     *
     * @param text   the text to parse
     * @param format the date format, see {@link java.text.SimpleDateFormat} for more information
     * @param locale the locale
     * @return the parsed date
     * @throws ParserException if the text cannot be parsed
     */
    public Date parse(String text, String format, Locale locale) throws ParserException {
      SimpleDateFormat dateFormat = getDateFormat(format, locale);

      try {
        return dateFormat.parse(text.trim());
      } catch (ParseException e) {
        throw new ParserException(
            "Error when parsing date(" + dateFormat.toPattern() + ") from " + text, e);
      }
    }

    private SimpleDateFormat getDateFormat(String format, Locale locale) {
      return new SimpleDateFormat(format, locale);
    }
  }

  public enum DurationParser {
    INSTANCE;

    private static final Pattern PATTERN =
        Pattern.compile(
            "(?:([0-9]+)D)?(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?(?:([0-9]+)(?:MS)?)?",
            Pattern.CASE_INSENSITIVE);

    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;
    private static final int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * HOURS_PER_DAY;

    public long parseToMillis(String text) throws ParserException {
      Matcher matcher = PATTERN.matcher(text);
      if (matcher.matches()) {
        String dayMatch = matcher.group(1);
        String hourMatch = matcher.group(2);
        String minuteMatch = matcher.group(3);
        String secondMatch = matcher.group(4);
        String fractionMatch = matcher.group(5);
        if (dayMatch != null
            || hourMatch != null
            || minuteMatch != null
            || secondMatch != null
            || fractionMatch != null) {
          int daysAsMilliSecs = parseNumber(dayMatch, MILLIS_PER_DAY);
          int hoursAsMilliSecs = parseNumber(hourMatch, MILLIS_PER_HOUR);
          int minutesAsMilliSecs = parseNumber(minuteMatch, MILLIS_PER_MINUTE);
          int secondsAsMilliSecs = parseNumber(secondMatch, MILLIS_PER_SECOND);
          int milliseconds = parseNumber(fractionMatch, 1);

          return daysAsMilliSecs
              + hoursAsMilliSecs
              + minutesAsMilliSecs
              + secondsAsMilliSecs
              + milliseconds;
        }
      }
      throw new ParserException(String.format("Text %s cannot be parsed to duration)", text));
    }

    private static int parseNumber(String parsed, int multiplier) {
      // regex limits to [0-9]+
      if (parsed == null || parsed.trim().isEmpty()) {
        return 0;
      }
      return Integer.parseInt(parsed) * multiplier;
    }
  }
}
