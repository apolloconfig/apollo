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
package com.ctrip.framework.apollo.util.parser;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DateParserTest {
  private Parsers.DateParser dateParser = Parsers.forDate();

  private String shortDateText = "2016-09-28";
  private String mediumDateText = "2016-09-28 15:10:10";
  private String longDateText = "2016-09-28 15:10:10.123";

  @Test
  public void testParseShortFormat() throws Exception {
    String format = "yyyy-MM-dd";
    Date expected = assembleDate(2016, 9, 28, 0, 0, 0, 0);

    checkWithFormat(expected, shortDateText, format);
    checkWithFormat(expected, mediumDateText, format);
    checkWithFormat(expected, longDateText, format);
  }

  @Test
  public void testParseMediumFormat() throws Exception {
    String format = "yyyy-MM-dd HH:mm:ss";
    Date expected = assembleDate(2016, 9, 28, 15, 10, 10, 0);

    checkWithFormat(expected, mediumDateText, format);
    checkWithFormat(expected, longDateText, format);
  }

  @Test
  public void testParseLongFormat() throws Exception {
    String format = "yyyy-MM-dd HH:mm:ss.SSS";
    Date expected = assembleDate(2016, 9, 28, 15, 10, 10, 123);

    checkWithFormat(expected, longDateText, format);
  }

  @Test
  public void testParseWithNoFormat() throws Exception {
    Date shortDate = assembleDate(2016, 9, 28, 0, 0, 0, 0);
    Date mediumDate = assembleDate(2016, 9, 28, 15, 10, 10, 0);
    Date longDate = assembleDate(2016, 9, 28, 15, 10, 10, 123);

    check(shortDate, shortDateText);
    check(mediumDate, mediumDateText);
    check(longDate, longDateText);
  }

  @Test
  public void testParseWithFormatAndLocale() throws Exception {
    Date someDate = assembleDate(2016, 9, 28, 15, 10, 10, 123);
    Locale someLocale = Locale.FRENCH;
    String someFormat = "EEE, d MMM yyyy HH:mm:ss.SSS Z";
    SimpleDateFormat someDateFormat = new SimpleDateFormat(someFormat, someLocale);
    String dateText = someDateFormat.format(someDate);

    checkWithFormatAndLocale(someDate, dateText, someFormat, someLocale);
  }

  @Test(expected = ParserException.class)
  public void testParseError() throws Exception {
    String someInvalidDate = "someInvalidDate";
    String format = "yyyy-MM-dd";

    dateParser.parse(someInvalidDate, format);
  }

  private void check(Date expected, String text) throws Exception {
    assertEquals(expected, dateParser.parse(text));
  }

  private void checkWithFormat(Date expected, String text, String format) throws Exception {
    assertEquals(expected, dateParser.parse(text, format));
  }

  private void checkWithFormatAndLocale(Date expected, String text, String format, Locale locale) throws Exception {
    assertEquals(expected, dateParser.parse(text, format, locale));
  }

  private Date assembleDate(int year, int month, int day, int hour, int minute, int second, int millisecond) {
    Calendar date = Calendar.getInstance();
    date.set(year, month - 1, day, hour, minute, second); //Month in Calendar is 0 based
    date.set(Calendar.MILLISECOND, millisecond);

    return date.getTime();
  }
}