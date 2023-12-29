/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.maven.extensions.sql;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class ApolloAssemblyMysqlConverterUtil {

 public static void convertAssemblyMysql(String srcSql, String targetSql,
     Map<String, SqlTemplate> templates) {

  ApolloSqlConverterUtil.ensureDirectories(targetSql);

  try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(srcSql),
      StandardCharsets.UTF_8);
      BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(targetSql),
          StandardCharsets.UTF_8, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING)) {
   for (String line = bufferedReader.readLine(); line != null;
       line = bufferedReader.readLine()) {
    String convertedLine = convertAssemblyMysqlLine(line, templates);
    bufferedWriter.write(convertedLine);
    bufferedWriter.write('\n');
   }
  } catch (IOException e) {
   throw new UncheckedIOException(e);
  }
 }

 private static String convertAssemblyMysqlLine(String line, Map<String, SqlTemplate> templates) {
  String convertedLine = line;
  ConvertResult result = ApolloSqlConverterUtil.convertTemplate(convertedLine,
      "auto-generated-declaration", templates);
  convertedLine = result.convertedLine();
  if (result.matches()) {
   return convertedLine;
  }
  convertedLine = ApolloSqlConverterUtil.convertTemplate(convertedLine, "h2-function", SqlTemplate.empty()).convertedLine();
  convertedLine = ApolloSqlConverterUtil.convertTemplate(convertedLine, "setup-database", templates).convertedLine();
  convertedLine = ApolloSqlConverterUtil.convertTemplate(convertedLine, "use-database", templates).convertedLine();
  return convertedLine;
 }
}
