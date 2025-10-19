/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.build.sql.converter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ApolloMysqlDefaultConverterUtil {

  public static void convert(SqlTemplate sqlTemplate, String targetSql,
      SqlTemplateContext context) {
    String databaseName;
    String srcSql = sqlTemplate.getSrcPath();
    if (srcSql.contains("apolloconfigdb")) {
      databaseName = "ApolloConfigDB";
    } else if (srcSql.contains("apolloportaldb")) {
      databaseName = "ApolloPortalDB";
    } else {
      throw new IllegalArgumentException("unknown database name: " + srcSql);
    }

    ApolloSqlConverterUtil.ensureDirectories(targetSql);

    String rawText = ApolloSqlConverterUtil.process(sqlTemplate, context);

    List<SqlStatement> sqlStatements = ApolloSqlConverterUtil.toStatements(rawText);
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(targetSql),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      for (SqlStatement sqlStatement : sqlStatements) {
        String convertedText = convertMainMysqlLine(sqlStatement, databaseName);
        bufferedWriter.write(convertedText);
        bufferedWriter.write('\n');
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String convertMainMysqlLine(SqlStatement sqlStatement, String databaseName) {
    String convertedText = sqlStatement.getRawText();

    convertedText = convertedText.replace("ApolloAssemblyDB", databaseName);

    return convertedText;
  }
}
