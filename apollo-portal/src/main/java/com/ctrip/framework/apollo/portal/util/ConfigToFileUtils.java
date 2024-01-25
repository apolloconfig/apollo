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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * jian.tan
 */
public class ConfigToFileUtils {

  @Deprecated
  public static void itemsToFile(OutputStream os, List<String> items) {
    try {
      PrintWriter printWriter = new PrintWriter(os);
      items.forEach(printWriter::println);
      printWriter.close();
    } catch (Exception e) {
      throw e;
    }
  }

  public static String fileToString(InputStream inputStream) {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
  }
}
