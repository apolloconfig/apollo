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
package com.ctrip.framework.apollo.util.function;

import java.util.Date;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.util.parser.ParserException;
import com.ctrip.framework.apollo.util.parser.Parsers;
import com.google.common.base.Function;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface Functions {
  Function<String, Integer> TO_INT_FUNCTION = Integer::parseInt;
  Function<String, Long> TO_LONG_FUNCTION = Long::parseLong;
  Function<String, Short> TO_SHORT_FUNCTION = Short::parseShort;
  Function<String, Float> TO_FLOAT_FUNCTION = Float::parseFloat;
  Function<String, Double> TO_DOUBLE_FUNCTION = Double::parseDouble;
  Function<String, Byte> TO_BYTE_FUNCTION = Byte::parseByte;
  Function<String, Boolean> TO_BOOLEAN_FUNCTION = Boolean::parseBoolean;
  Function<String, Date> TO_DATE_FUNCTION = input -> {
    try {
      return Parsers.forDate().parse(input);
    } catch (ParserException ex) {
      throw new ApolloConfigException("Parse date failed", ex);
    }
  };
  Function<String, Long> TO_DURATION_FUNCTION = input -> {
    try {
      return Parsers.forDuration().parseToMillis(input);
    } catch (ParserException ex) {
      throw new ApolloConfigException("Parse duration failed", ex);
    }
  };
}
