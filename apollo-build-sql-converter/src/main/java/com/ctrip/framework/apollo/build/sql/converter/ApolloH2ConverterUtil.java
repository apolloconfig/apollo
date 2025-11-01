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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApolloH2ConverterUtil {

  public static void convert(SqlTemplate sqlTemplate, String targetSql,
      SqlTemplateContext context) {

    ApolloSqlConverterUtil.ensureDirectories(targetSql);

    String rawText = ApolloSqlConverterUtil.process(sqlTemplate, context);

    List<SqlStatement> sqlStatements = ApolloSqlConverterUtil.toStatements(rawText);
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(targetSql),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      for (SqlStatement sqlStatement : sqlStatements) {
        String convertedText;
        try {
          convertedText = convertAssemblyH2Line(sqlStatement);
        } catch (Throwable e) {
          throw new RuntimeException("convert error: " + sqlStatement.getRawText(), e);
        }
        bufferedWriter.write(convertedText);
        bufferedWriter.write('\n');
      }

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }


  private static final Pattern OPERATION_TABLE_PATTERN = Pattern.compile(
      "(?<operation>DROP|CREATE|ALTER)\\s+TABLE\\s+(`)?(?<tableName>[a-zA-Z0-9\\-_]+)(`)?\\s*",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern CREATE_INDEX_ON_PATTERN = Pattern.compile(
      "CREATE\\s+INDEX\\s+(`)?(?<indexName>[a-zA-Z0-9\\-_]+)(`)?\\s+ON\\s+(`)?(?<tableName>[a-zA-Z0-9\\-_]+)(`)?",
      Pattern.CASE_INSENSITIVE);

  private static String convertAssemblyH2Line(SqlStatement sqlStatement) {
    String convertedText = sqlStatement.getRawText();

    // <operation> TABLE `<tableName>`
    Matcher opTableMatcher = OPERATION_TABLE_PATTERN.matcher(convertedText);
    if (opTableMatcher.find()) {
      String operation = opTableMatcher.group("operation");
      if ("DROP".equalsIgnoreCase(operation)) {
        return "";
      } else if ("CREATE".equalsIgnoreCase(operation)) {
        return convertCreateTable(convertedText, sqlStatement, opTableMatcher);
      } else if ("ALTER".equalsIgnoreCase(operation)) {
        return convertAlterTable(convertedText, sqlStatement, opTableMatcher);
      }
    }

    // CREATE INDEX `<indexName>` ON `<tableName>`
    Matcher createIndexOnMatcher = CREATE_INDEX_ON_PATTERN.matcher(convertedText);
    if (createIndexOnMatcher.find()) {
      String createIndexOnTableName = createIndexOnMatcher.group("tableName");
      // index with table
      return convertIndexOnTable(convertedText, createIndexOnTableName, sqlStatement);
    }

    // others
    return convertedText;
  }

  private static String convertCreateTable(String convertedText, SqlStatement sqlStatement,
      Matcher opTableMatcher) {
    String tableName = opTableMatcher.group("tableName");
    // table config
    convertedText = convertTableConfig(convertedText, sqlStatement);
    // index with table
    convertedText = convertIndexWithTable(convertedText, tableName, sqlStatement);
    // column
    convertedText = convertColumn(convertedText, sqlStatement);
    return convertedText;
  }

  private static final Pattern ENGINE_PATTERN =
      Pattern.compile("ENGINE\\s*=\\s*InnoDB", Pattern.CASE_INSENSITIVE);

  private static final Pattern DEFAULT_CHARSET_PATTERN =
      Pattern.compile("DEFAULT\\s+CHARSET\\s*=\\s*utf8mb4", Pattern.CASE_INSENSITIVE);

  private static final Pattern ROW_FORMAT_PATTERN =
      Pattern.compile("ROW_FORMAT\\s*=\\s*DYNAMIC", Pattern.CASE_INSENSITIVE);

  private static String convertTableConfig(String convertedText, SqlStatement sqlStatement) {
    Matcher engineMatcher = ENGINE_PATTERN.matcher(convertedText);
    if (engineMatcher.find()) {
      convertedText = engineMatcher.replaceAll("");
    }
    Matcher defaultCharsetMatcher = DEFAULT_CHARSET_PATTERN.matcher(convertedText);
    if (defaultCharsetMatcher.find()) {
      convertedText = defaultCharsetMatcher.replaceAll("");
    }
    Matcher rowFormatMatcher = ROW_FORMAT_PATTERN.matcher(convertedText);
    if (rowFormatMatcher.find()) {
      convertedText = rowFormatMatcher.replaceAll("");
    }
    return convertedText;
  }

  private static final Pattern INDEX_NAME_PATTERN = Pattern.compile(
      // KEY `AppId_ClusterName_GroupName`
      "(KEY\\s*`|KEY\\s+)(?<indexName>[a-zA-Z0-9\\-_]+)(`)?\\s*"
          // (`AppId`,`ClusterName`(191),`NamespaceName`(191))
          + "\\((?<indexColumns>" + "(`)?[a-zA-Z0-9\\-_]+(`)?\\s*(\\([0-9]+\\))?" + "(,"
          + "(`)?[a-zA-Z0-9\\-_]+(`)?\\s*(\\([0-9]+\\))?" + ")*" + ")\\)",
      Pattern.CASE_INSENSITIVE);

  private static String convertIndexWithTable(String convertedText, String tableName,
      SqlStatement sqlStatement) {
    String[] lines = convertedText.split("\n");
    StringJoiner joiner = new StringJoiner("\n");
    for (String line : lines) {
      String convertedLine = line;
      if (convertedLine.contains("KEY") || convertedLine.contains("key")) {
        // replace index name
        // KEY `AppId_ClusterName_GroupName` (`AppId`,`ClusterName`(191),`NamespaceName`(191))
        // ->
        // KEY `tableName_AppId_ClusterName_GroupName`
        // (`AppId`,`ClusterName`(191),`NamespaceName`(191))
        Matcher indexNameMatcher = INDEX_NAME_PATTERN.matcher(convertedLine);
        if (indexNameMatcher.find()) {
          convertedLine =
              indexNameMatcher.replaceAll("KEY `" + tableName + "_${indexName}` (${indexColumns})");
        }
        convertedLine = removePrefixIndex(convertedLine);
      }
      joiner.add(convertedLine);
    }
    return joiner.toString();
  }

  private static String convertColumn(String convertedText, SqlStatement sqlStatement) {
    // convert bit(1) to boolean
    // `IsDeleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '1: deleted, 0: normal'
    // ->
    // `IsDeleted` boolean NOT NULL DEFAULT FALSE
    if (convertedText.contains("bit(1)")) {
      convertedText = convertedText.replace("bit(1)", "boolean");
    }
    if (convertedText.contains("b'0'")) {
      convertedText = convertedText.replace("b'0'", "FALSE");
    }
    if (convertedText.contains("b'1'")) {
      convertedText = convertedText.replace("b'1'", "TRUE");
    }

    return convertedText;
  }

  private static String convertAlterTable(String convertedText, SqlStatement sqlStatement,
      Matcher opTableMatcher) {
    String tableName = opTableMatcher.group("tableName");
    // remove first table name
    convertedText = opTableMatcher.replaceAll("");
    convertedText = convertAlterTableMulti(convertedText, sqlStatement, tableName);

    return convertedText;
  }

  private static final Pattern ADD_COLUMN_PATTERN = Pattern.compile(
      "\\s*ADD\\s+COLUMN\\s+(`)?(?<columnName>[a-zA-Z0-9\\-_]+)(`)?(?<subStatement>.*)[,;]",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern MODIFY_COLUMN_PATTERN = Pattern.compile(
      "\\s*MODIFY\\s+COLUMN\\s+(`)?(?<columnName>[a-zA-Z0-9\\-_]+)(`)?(?<subStatement>.*)[,;]",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern CHANGE_PATTERN = Pattern.compile(
      "\\s*CHANGE\\s+(`)?(?<oldColumnName>[a-zA-Z0-9\\-_]+)(`)?\\s+(`)?(?<newColumnName>[a-zA-Z0-9\\-_]+)(`)?(?<subStatement>.*)[,;]",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern DROP_COLUMN_PATTERN =
      Pattern.compile("\\s*DROP\\s+(COLUMN\\s+)?(`)?(?<columnName>[a-zA-Z0-9\\-_]+)(`)?\\s*[,;]",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern ADD_KEY_PATTERN = Pattern.compile(
      "\\s*ADD\\s+(?<indexType>(UNIQUE\\s+)?KEY)\\s+(`)?(?<indexName>[a-zA-Z0-9\\-_]+)(`)?(?<subStatement>.*)[,;]",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ADD_INDEX_PATTERN = Pattern.compile(
      "\\s*ADD\\s+(?<indexType>(UNIQUE\\s+)?INDEX)\\s+(`)?(?<indexName>[a-zA-Z0-9\\-_]+)(`)?(?<subStatement>.*)[,;]",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern DROP_INDEX_PATTERN =
      Pattern.compile("\\s*DROP\\s+INDEX\\s+(`)?(?<indexName>[a-zA-Z0-9\\-_]+)(`)?\\s*[,;]",
          Pattern.CASE_INSENSITIVE);

  private static String convertAlterTableMulti(String convertedText, SqlStatement sqlStatement,
      String tableName) {
    Matcher addColumnMatcher = ADD_COLUMN_PATTERN.matcher(convertedText);
    if (addColumnMatcher.find()) {
      convertedText = addColumnMatcher.replaceAll(
          "\nALTER TABLE `" + tableName + "` ADD COLUMN `${columnName}`${subStatement};");
    }
    Matcher modifyColumnMatcher = MODIFY_COLUMN_PATTERN.matcher(convertedText);
    if (modifyColumnMatcher.find()) {
      convertedText = modifyColumnMatcher.replaceAll(
          "\nALTER TABLE `" + tableName + "` MODIFY COLUMN `${columnName}`${subStatement};");
    }
    Matcher changeMatcher = CHANGE_PATTERN.matcher(convertedText);
    if (changeMatcher.find()) {
      convertedText = changeMatcher.replaceAll("\nALTER TABLE `" + tableName
          + "` CHANGE `${oldColumnName}` `${newColumnName}` ${subStatement};");
    }

    Matcher dropColumnMatcher = DROP_COLUMN_PATTERN.matcher(convertedText);
    if (dropColumnMatcher.find()) {
      convertedText =
          dropColumnMatcher.replaceAll("\nALTER TABLE `" + tableName + "` DROP `${columnName}`;");
    }
    Matcher addKeyMatcher = ADD_KEY_PATTERN.matcher(convertedText);
    if (addKeyMatcher.find()) {
      convertedText = addKeyMatcher.replaceAll("\nALTER TABLE `" + tableName
          + "` ADD ${indexType} `" + tableName + "_${indexName}` ${subStatement};");
      convertedText = removePrefixIndex(convertedText);
    }
    Matcher addIndexMatcher = ADD_INDEX_PATTERN.matcher(convertedText);
    if (addIndexMatcher.find()) {
      convertedText = addIndexMatcher.replaceAll("\nALTER TABLE `" + tableName
          + "` ADD ${indexType} `" + tableName + "_${indexName}` ${subStatement};");
      convertedText = removePrefixIndex(convertedText);
    }
    Matcher dropIndexMatcher = DROP_INDEX_PATTERN.matcher(convertedText);
    if (dropIndexMatcher.find()) {
      convertedText = dropIndexMatcher.replaceAll(
          "\nALTER TABLE `" + tableName + "` DROP INDEX `" + tableName + "_${indexName}`;");
    }
    return convertedText;
  }

  private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile(
      "CREATE\\s+(?<indexType>(UNIQUE\\s+)?INDEX)\\s+(`)?(?<indexName>[a-zA-Z0-9\\-_]+)(`)?",
      Pattern.CASE_INSENSITIVE);

  private static String convertIndexOnTable(String convertedText, String tableName,
      SqlStatement sqlStatement) {
    Matcher createIndexMatcher = CREATE_INDEX_PATTERN.matcher(convertedText);
    if (createIndexMatcher.find()) {
      convertedText =
          createIndexMatcher.replaceAll("CREATE ${indexType} `" + tableName + "_${indexName}`");
      convertedText = removePrefixIndex(convertedText);
    }
    return convertedText;
  }

  private static final Pattern PREFIX_INDEX_PATTERN = Pattern.compile("(?<prefix>\\("
      // other columns
      + "((`)?[a-zA-Z0-9\\-_]+(`)?\\s*(\\([0-9]+\\))?,)*)"
      // `<columnName>`(191)
      + "(`)?(?<columnName>[a-zA-Z0-9\\-_]+)(`)?\\s*\\([0-9]+\\)"
      // other columns
      + "(?<suffix>(,(`)?[a-zA-Z0-9\\-_]+(`)?\\s*(\\([0-9]+\\))?)*" + "\\))");

  private static String removePrefixIndex(String convertedText) {
    // convert prefix index
    // (`AppId`,`ClusterName`(191),`NamespaceName`(191))
    // ->
    // (`AppId`,`ClusterName`,`NamespaceName`)
    for (Matcher prefixIndexMatcher =
        PREFIX_INDEX_PATTERN.matcher(convertedText); prefixIndexMatcher
            .find(); prefixIndexMatcher = PREFIX_INDEX_PATTERN.matcher(convertedText)) {
      convertedText = prefixIndexMatcher.replaceAll("${prefix}`${columnName}`${suffix}");
    }
    return convertedText;
  }
}
