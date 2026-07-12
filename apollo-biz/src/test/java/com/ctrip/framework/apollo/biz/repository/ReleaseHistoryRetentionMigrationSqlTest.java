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
package com.ctrip.framework.apollo.biz.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Verifies the manual release history retention scripts against H2 in MySQL mode. */
public class ReleaseHistoryRetentionMigrationSqlTest {

  private static final String SCRIPT_DIRECTORY = "scripts/sql/migration/release-history-retention";

  private Connection connection;
  private DataSource dataSource;
  private JdbcTemplate jdbcTemplate;
  private Path scriptDirectory;

  @Before
  public void setUp() throws SQLException {
    DriverManagerDataSource testDataSource = new DriverManagerDataSource();
    testDataSource.setDriverClassName("org.h2.Driver");
    testDataSource.setUrl("jdbc:h2:mem:release-history-retention-" + UUID.randomUUID()
        + ";MODE=MySQL;DATABASE_TO_UPPER=FALSE");
    testDataSource.setUsername("sa");
    dataSource = testDataSource;
    connection = dataSource.getConnection();
    jdbcTemplate = new JdbcTemplate(dataSource);
    scriptDirectory = findScriptDirectory();
    createSchema();
  }

  @After
  public void tearDown() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void precheckScriptsReportRepairAndCleanupScope() throws Exception {
    insertMigrationFixture();

    List<List<Map<String, Object>>> restorePrecheck =
        executeScript("01-precheck-releases-needing-restore.sql");
    assertEquals(2, restorePrecheck.size());
    assertEquals(3, longValue(restorePrecheck.get(0).get(0), "ReleasesNeedingRestore"));
    assertEquals(0, longValue(restorePrecheck.get(0).get(0), "ActiveReleaseKeyConflicts"));

    Map<Long, Map<String, Object>> restoreCandidates = restorePrecheck.get(1).stream()
        .collect(Collectors.toMap(row -> longValue(row, "Id"), row -> row));
    assertEquals(3, restoreCandidates.size());
    assertTrue(booleanValue(restoreCandidates.get(1L), "ReferencedByReleaseHistory"));
    assertTrue(booleanValue(restoreCandidates.get(2L), "ReferencedAsPreviousRelease"));
    assertTrue(booleanValue(restoreCandidates.get(3L), "ReferencedByGrayReleaseRule"));

    List<List<Map<String, Object>>> cleanupPrecheck =
        executeScript("02-precheck-soft-deleted-rows.sql");
    assertEquals(4, cleanupPrecheck.size());
    assertEquals(1, longValue(cleanupPrecheck.get(0).get(0), "SoftDeletedReleaseHistoryRows"));
    assertEquals(5, longValue(cleanupPrecheck.get(1).get(0), "SoftDeletedReleaseRows"));
    assertEquals(1,
        longValue(cleanupPrecheck.get(2).get(0), "SoftDeletedReleaseRowsEligibleForDeletion"));
    assertEquals(1, cleanupPrecheck.get(3).size());
    assertEquals(1, longValue(cleanupPrecheck.get(3).get(0), "SoftDeletedRows"));
  }

  @Test
  public void writeScriptsRestoreAndPurgeSafelyAndAreIdempotent() throws Exception {
    insertMigrationFixture();

    List<List<Map<String, Object>>> restoreResult =
        executeScript("03-restore-referenced-releases.sql");
    assertEquals(0, longValue(restoreResult.get(0).get(0), "RemainingReleasesNeedingRestore"));
    for (long releaseId : Arrays.asList(1L, 2L, 3L)) {
      assertFalse(isReleaseDeleted(releaseId));
      assertEquals(0, releaseDeletedAt(releaseId));
      assertEquals("release-history-retention-migration", releaseLastModifiedBy(releaseId));
    }
    assertTrue(isReleaseDeleted(4));
    assertTrue(isReleaseDeleted(5));

    List<List<Map<String, Object>>> purgeResult = executeScript("04-purge-soft-deleted-rows.sql");
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingSoftDeletedReleaseHistoryRows"));
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingSoftDeletedReleaseRows"));
    assertEquals(1, countReleaseHistories());
    assertEquals(4, countReleases());
    assertTrue(releaseExists(1));
    assertTrue(releaseExists(2));
    assertTrue(releaseExists(3));
    assertFalse(releaseExists(4));
    assertFalse(releaseExists(5));
    assertTrue(releaseExists(6));

    assertEquals(0, longValue(executeScript("03-restore-referenced-releases.sql").get(0).get(0),
        "RemainingReleasesNeedingRestore"));
    List<List<Map<String, Object>>> secondPurge = executeScript("04-purge-soft-deleted-rows.sql");
    assertEquals(0, longValue(secondPurge.get(0).get(0), "RemainingSoftDeletedReleaseHistoryRows"));
    assertEquals(0, longValue(secondPurge.get(0).get(0), "RemainingSoftDeletedReleaseRows"));
    assertEquals(1, countReleaseHistories());
    assertEquals(4, countReleases());
  }

  @Test
  public void restorePrecheckDetectsReleaseKeyConflictAndRestoreFailsSafely() throws Exception {
    insertMigrationFixture();
    insertRelease(7, "release-1", false, 0);

    List<List<Map<String, Object>>> precheck =
        executeScript("01-precheck-releases-needing-restore.sql");
    assertEquals(1, longValue(precheck.get(0).get(0), "ActiveReleaseKeyConflicts"));
    assertTrue(booleanValue(precheck.get(1).get(0), "ActiveReleaseKeyConflict"));

    assertThrows(SQLException.class, () -> executeScript("03-restore-referenced-releases.sql"));
    assertTrue(isReleaseDeleted(1));
    assertTrue(isReleaseDeleted(2));
    assertTrue(isReleaseDeleted(3));
    assertEquals(5, countSoftDeletedReleases());
  }

  @Test
  public void writeScriptsUseBoundedBatches() throws IOException {
    assertEquals(1, occurrences(readScript("03-restore-referenced-releases.sql"), "LIMIT 1000"));
    assertEquals(2, occurrences(readScript("04-purge-soft-deleted-rows.sql"), "LIMIT 1000"));
  }

  private void createSchema() {
    jdbcTemplate.execute(
        "CREATE TABLE `Release` (" + "`Id` BIGINT PRIMARY KEY, `ReleaseKey` VARCHAR(64) NOT NULL, "
            + "`AppId` VARCHAR(64) NOT NULL, `ClusterName` VARCHAR(32) NOT NULL, "
            + "`NamespaceName` VARCHAR(32) NOT NULL, `IsDeleted` BOOLEAN NOT NULL, "
            + "`DeletedAt` BIGINT NOT NULL, `DataChange_LastModifiedBy` VARCHAR(64), "
            + "UNIQUE (`ReleaseKey`, `DeletedAt`))");
    jdbcTemplate.execute("CREATE TABLE `ReleaseHistory` ("
        + "`Id` BIGINT PRIMARY KEY, `AppId` VARCHAR(64) NOT NULL, "
        + "`ClusterName` VARCHAR(32) NOT NULL, `NamespaceName` VARCHAR(32) NOT NULL, "
        + "`BranchName` VARCHAR(32) NOT NULL, `ReleaseId` BIGINT NOT NULL, "
        + "`PreviousReleaseId` BIGINT NOT NULL, `IsDeleted` BOOLEAN NOT NULL)");
    jdbcTemplate.execute("CREATE INDEX `IX_ReleaseId` ON `ReleaseHistory` (`ReleaseId`)");
    jdbcTemplate
        .execute("CREATE INDEX `IX_PreviousReleaseId` ON `ReleaseHistory` (`PreviousReleaseId`)");
    jdbcTemplate.execute("CREATE TABLE `GrayReleaseRule` ("
        + "`Id` BIGINT PRIMARY KEY, `ReleaseId` BIGINT NOT NULL, "
        + "`IsDeleted` BOOLEAN NOT NULL)");
  }

  private void insertMigrationFixture() {
    insertRelease(1, "release-1", true, 1);
    insertRelease(2, "release-2", true, 2);
    insertRelease(3, "release-3", true, 3);
    insertRelease(4, "release-4", true, 4);
    insertRelease(5, "release-5", true, 5);
    insertRelease(6, "release-6", false, 0);
    insertReleaseHistory(1, 1, 2, false);
    insertReleaseHistory(2, 4, 0, true);
    jdbcTemplate.update(
        "INSERT INTO `GrayReleaseRule` (`Id`, `ReleaseId`, `IsDeleted`) " + "VALUES (1, 3, FALSE)");
  }

  private void insertRelease(long id, String releaseKey, boolean deleted, long deletedAt) {
    jdbcTemplate.update(
        "INSERT INTO `Release` (`Id`, `ReleaseKey`, `AppId`, `ClusterName`, "
            + "`NamespaceName`, `IsDeleted`, `DeletedAt`, `DataChange_LastModifiedBy`) "
            + "VALUES (?, ?, 'app', 'default', 'application', ?, ?, '')",
        id, releaseKey, deleted, deletedAt);
  }

  private void insertReleaseHistory(long id, long releaseId, long previousReleaseId,
      boolean deleted) {
    jdbcTemplate.update(
        "INSERT INTO `ReleaseHistory` (`Id`, `AppId`, `ClusterName`, "
            + "`NamespaceName`, `BranchName`, `ReleaseId`, `PreviousReleaseId`, `IsDeleted`) "
            + "VALUES (?, 'app', 'default', 'application', 'default', ?, ?, ?)",
        id, releaseId, previousReleaseId, deleted);
  }

  private List<List<Map<String, Object>>> executeScript(String scriptName)
      throws IOException, SQLException {
    List<List<Map<String, Object>>> queryResults = new ArrayList<>();
    try (Statement statement = connection.createStatement()) {
      for (String sql : readStatements(scriptDirectory.resolve(scriptName))) {
        if (statement.execute(sql)) {
          try (ResultSet resultSet = statement.getResultSet()) {
            queryResults.add(readRows(resultSet));
          }
        }
      }
    } catch (SQLException ex) {
      if (!connection.getAutoCommit()) {
        connection.rollback();
      }
      connection.setAutoCommit(true);
      throw ex;
    }
    return queryResults;
  }

  private List<String> readStatements(Path script) throws IOException {
    String sql = Files.readAllLines(script, StandardCharsets.UTF_8).stream()
        .filter(line -> !line.trim().startsWith("--")).collect(Collectors.joining("\n"));
    return Arrays.stream(sql.split(";")).map(String::trim).filter(statement -> !statement.isEmpty())
        .collect(Collectors.toList());
  }

  private String readScript(String scriptName) throws IOException {
    return Files.readString(scriptDirectory.resolve(scriptName), StandardCharsets.UTF_8);
  }

  private int occurrences(String value, String token) {
    return value.split(token, -1).length - 1;
  }

  private List<Map<String, Object>> readRows(ResultSet resultSet) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    ResultSetMetaData metadata = resultSet.getMetaData();
    while (resultSet.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int column = 1; column <= metadata.getColumnCount(); column++) {
        row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
      }
      rows.add(row);
    }
    return rows;
  }

  private Path findScriptDirectory() {
    Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(SCRIPT_DIRECTORY);
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Could not locate " + SCRIPT_DIRECTORY);
  }

  private boolean isReleaseDeleted(long releaseId) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "SELECT `IsDeleted` FROM `Release` WHERE `Id` = ?", Boolean.class, releaseId));
  }

  private long releaseDeletedAt(long releaseId) {
    return jdbcTemplate.queryForObject("SELECT `DeletedAt` FROM `Release` WHERE `Id` = ?",
        Long.class, releaseId);
  }

  private String releaseLastModifiedBy(long releaseId) {
    return jdbcTemplate.queryForObject(
        "SELECT `DataChange_LastModifiedBy` FROM `Release` WHERE `Id` = ?", String.class,
        releaseId);
  }

  private boolean releaseExists(long releaseId) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `Release` WHERE `Id` = ?",
        Integer.class, releaseId) > 0;
  }

  private int countSoftDeletedReleases() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `Release` WHERE `IsDeleted` = TRUE",
        Integer.class);
  }

  private int countReleases() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `Release`", Integer.class);
  }

  private int countReleaseHistories() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `ReleaseHistory`", Integer.class);
  }

  private long longValue(Map<String, Object> row, String column) {
    return ((Number) row.get(column)).longValue();
  }

  private boolean booleanValue(Map<String, Object> row, String column) {
    Object value = row.get(column);
    return value instanceof Boolean ? (Boolean) value : ((Number) value).intValue() != 0;
  }
}
