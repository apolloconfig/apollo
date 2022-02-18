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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DurationParserTest {
  private Parsers.DurationParser durationParser = Parsers.forDuration();

  @Test
  public void testParseMilliSeconds() throws Exception {
    String text = "345MS";
    long expected = 345;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseMilliSecondsWithNoSuffix() throws Exception {
    String text = "123";
    long expected = 123;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseSeconds() throws Exception {
    String text = "20S";
    long expected = 20 * 1000;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseMinutes() throws Exception {
    String text = "15M";
    long expected = 15 * 60 * 1000;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseHours() throws Exception {
    String text = "10H";
    long expected = 10 * 3600 * 1000;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseDays() throws Exception {
    String text = "2D";
    long expected = 2 * 24 * 3600 * 1000;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseFullText() throws Exception {
    String text = "2D3H4M5S123MS";
    long expected = 2 * 24 * 3600 * 1000 + 3 * 3600 * 1000 + 4 * 60 * 1000 + 5 * 1000 + 123;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseFullTextWithLowerCase() throws Exception {
    String text = "2d3h4m5s123ms";
    long expected = 2 * 24 * 3600 * 1000 + 3 * 3600 * 1000 + 4 * 60 * 1000 + 5 * 1000 + 123;

    checkParseToMillis(expected, text);
  }

  @Test
  public void testParseFullTextWithNoMS() throws Exception {
    String text = "2D3H4M5S123";
    long expected = 2 * 24 * 3600 * 1000 + 3 * 3600 * 1000 + 4 * 60 * 1000 + 5 * 1000 + 123;

    checkParseToMillis(expected, text);
  }

  @Test(expected = ParserException.class)
  public void testParseException() throws Exception {
    String text = "someInvalidText";

    durationParser.parseToMillis(text);
  }

  private void checkParseToMillis(long expected, String text) throws Exception {
    assertEquals(expected, durationParser.parseToMillis(text));
  }
}