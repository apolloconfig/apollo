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
package com.ctrip.framework.apollo.core.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class PropertiesUtil {
  /**
   * Transform the properties to string format
   * @param properties the properties object
   * @return the string containing the properties
   * @throws IOException
   */
  public static String toString(Properties properties) throws IOException {
    StringWriter writer = new StringWriter();
    properties.store(writer, null);
    StringBuffer stringBuffer = writer.getBuffer();
    filterPropertiesComment(stringBuffer);
    return stringBuffer.toString();
  }

  /**
   * filter out the first comment line
   * @param stringBuffer the string buffer
   * @return true if filtered successfully, false otherwise
   */
  static boolean filterPropertiesComment(StringBuffer stringBuffer) {
    // check whether has comment in the first line
    if (stringBuffer.charAt(0) != '#') {
      return false;
    }
    int commentLineIndex = stringBuffer.indexOf("\n");
    if (commentLineIndex == -1) {
      return false;
    }
    stringBuffer.delete(0, commentLineIndex + 1);
    return true;
  }
}
