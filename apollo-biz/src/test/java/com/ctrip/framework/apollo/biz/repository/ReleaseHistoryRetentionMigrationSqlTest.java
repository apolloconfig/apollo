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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Verifies the manual release history retention scripts against H2 in MySQL mode. */
public class ReleaseHistoryRetentionMigrationSqlTest {

  private static final String SCRIPT_DIRECTORY = "scripts/sql/migration/release-history-retention";

  private Connection connection;
  private DataSource dataSource;
  private JdbcTemplate jdbcTemplate;
  private Path scriptDirectory;

  @BeforeEach
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

  @AfterEach
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
    assertEquals(1,
        longValue(cleanupPrecheck.get(0).get(0), "RetentionReleaseHistoryPurgeCandidates"));
    assertEquals(0,
        longValue(cleanupPrecheck.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(5, longValue(cleanupPrecheck.get(1).get(0), "SoftDeletedReleaseRows"));
    assertEquals(5,
        longValue(cleanupPrecheck.get(1).get(0), "RetentionScopedSoftDeletedReleaseRows"));
    assertEquals(0, longValue(cleanupPrecheck.get(1).get(0), "ExcludedSoftDeletedReleaseRows"));
    assertEquals(1,
        longValue(cleanupPrecheck.get(2).get(0), "RetentionReleaseRowsEligibleForDeletion"));
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
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingRetentionReleaseHistoryRows"));
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingRetentionReleaseRows"));
    assertEquals(0, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(0, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseRows"));
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
    assertEquals(0, longValue(secondPurge.get(0).get(0), "RemainingRetentionReleaseHistoryRows"));
    assertEquals(0, longValue(secondPurge.get(0).get(0), "RemainingRetentionReleaseRows"));
    assertEquals(1, countReleaseHistories());
    assertEquals(4, countReleases());
  }

  @Test
  public void inactiveGrayReleaseRulesDoNotRestoreOrProtectReleases() throws Exception {
    insertMigrationFixture();
    insertRelease(7, "release-7", true, 7);
    insertRelease(8, "release-8", true, 8);
    insertGrayReleaseRule(2, 7, 0);
    insertGrayReleaseRule(3, 8, 2);

    List<List<Map<String, Object>>> precheck =
        executeScript("01-precheck-releases-needing-restore.sql");
    assertEquals(3, longValue(precheck.get(0).get(0), "ReleasesNeedingRestore"));
    assertFalse(precheck.get(1).stream().anyMatch(row -> longValue(row, "Id") == 7));
    assertFalse(precheck.get(1).stream().anyMatch(row -> longValue(row, "Id") == 8));

    executeScript("03-restore-referenced-releases.sql");
    assertTrue(isReleaseDeleted(7));
    assertTrue(isReleaseDeleted(8));

    executeScript("04-purge-soft-deleted-rows.sql");
    assertFalse(releaseExists(7));
    assertFalse(releaseExists(8));
  }

  @Test
  public void purgeExcludesNamespaceDeletionAndPreviousNamespaceIncarnation() throws Exception {
    insertMigrationFixture();
    insertNamespace(2, "deleted-namespace", true, 7, "2026-01-01 00:00:00");
    insertRelease(7, "release-7", "deleted-namespace", true, 7, "2026-02-01 00:00:00");
    insertReleaseHistory(3, "deleted-namespace", 7, 0, true, "2026-02-01 00:00:00");

    insertNamespace(3, "recreated-namespace", true, 8, "2026-02-01 00:00:00");
    insertNamespace(4, "recreated-namespace", false, 0, "2026-03-01 00:00:00");
    insertRelease(8, "release-8", "recreated-namespace", true, 8, "2026-02-01 00:00:00");
    insertReleaseHistory(4, "recreated-namespace", 8, 0, true, "2026-02-01 00:00:00");

    List<List<Map<String, Object>>> precheck = executeScript("02-precheck-soft-deleted-rows.sql");
    assertEquals(3, longValue(precheck.get(0).get(0), "SoftDeletedReleaseHistoryRows"));
    assertEquals(1, longValue(precheck.get(0).get(0), "RetentionReleaseHistoryPurgeCandidates"));
    assertEquals(2, longValue(precheck.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(7, longValue(precheck.get(1).get(0), "SoftDeletedReleaseRows"));
    assertEquals(5, longValue(precheck.get(1).get(0), "RetentionScopedSoftDeletedReleaseRows"));
    assertEquals(2, longValue(precheck.get(1).get(0), "ExcludedSoftDeletedReleaseRows"));

    executeScript("03-restore-referenced-releases.sql");
    List<List<Map<String, Object>>> purgeResult = executeScript("04-purge-soft-deleted-rows.sql");
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingRetentionReleaseHistoryRows"));
    assertEquals(0, longValue(purgeResult.get(0).get(0), "RemainingRetentionReleaseRows"));
    assertEquals(2, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(2, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseRows"));
    assertTrue(releaseHistoryExists(3));
    assertTrue(releaseHistoryExists(4));
    assertTrue(releaseExists(7));
    assertTrue(releaseExists(8));
  }

  @Test
  public void scriptsUseDeletedAtForSameSecondNamespaceRecreation() throws Exception {
    insertMigrationFixture();
    String sameSecond = "2026-04-01 00:00:00";
    insertNamespace(2, "same-second", true, 1002, sameSecond);
    insertNamespace(3, "same-second", false, 0, sameSecond);
    insertRelease(7, "release-7", "same-second", true, 1001, sameSecond);
    insertReleaseHistory(3, "same-second", 7, 0, true, 1001, sameSecond);
    insertRelease(8, "release-8", "same-second", true, 1003, sameSecond);
    insertReleaseHistory(4, "same-second", 8, 0, true, 1003, sameSecond);
    insertRelease(9, "release-9", "same-second", true, 1001, sameSecond);
    insertReleaseHistory(5, "same-second", 9, 0, false, 0, sameSecond);

    List<List<Map<String, Object>>> restorePrecheck =
        executeScript("01-precheck-releases-needing-restore.sql");
    assertEquals(3, longValue(restorePrecheck.get(0).get(0), "ReleasesNeedingRestore"));
    assertFalse(restorePrecheck.get(1).stream().anyMatch(row -> longValue(row, "Id") == 9));

    List<List<Map<String, Object>>> precheck = executeScript("02-precheck-soft-deleted-rows.sql");
    assertEquals(3, longValue(precheck.get(0).get(0), "SoftDeletedReleaseHistoryRows"));
    assertEquals(2, longValue(precheck.get(0).get(0), "RetentionReleaseHistoryPurgeCandidates"));
    assertEquals(1, longValue(precheck.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(8, longValue(precheck.get(1).get(0), "SoftDeletedReleaseRows"));
    assertEquals(6, longValue(precheck.get(1).get(0), "RetentionScopedSoftDeletedReleaseRows"));
    assertEquals(2, longValue(precheck.get(1).get(0), "ExcludedSoftDeletedReleaseRows"));

    executeScript("03-restore-referenced-releases.sql");
    assertTrue(isReleaseDeleted(9));
    List<List<Map<String, Object>>> purgeResult = executeScript("04-purge-soft-deleted-rows.sql");
    assertEquals(1, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseHistoryRows"));
    assertEquals(2, longValue(purgeResult.get(0).get(0), "ExcludedSoftDeletedReleaseRows"));
    assertTrue(releaseHistoryExists(3));
    assertFalse(releaseHistoryExists(4));
    assertTrue(releaseExists(7));
    assertFalse(releaseExists(8));
    assertTrue(releaseExists(9));
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

  @Test
  public void scriptsForceReleaseHistoryReferenceIndexes() throws IOException {
    assertReleaseHistoryIndexHints("01-precheck-releases-needing-restore.sql", 3);
    assertReleaseHistoryIndexHints("02-precheck-soft-deleted-rows.sql", 1);
    assertReleaseHistoryIndexHints("03-restore-referenced-releases.sql", 2);
    assertReleaseHistoryIndexHints("04-purge-soft-deleted-rows.sql", 1);
  }

  private void createSchema() {
    jdbcTemplate.execute(
        "CREATE TABLE `Namespace` (" + "`Id` BIGINT PRIMARY KEY, `AppId` VARCHAR(64) NOT NULL, "
            + "`ClusterName` VARCHAR(32) NOT NULL, `NamespaceName` VARCHAR(32) NOT NULL, "
            + "`IsDeleted` BOOLEAN NOT NULL, `DeletedAt` BIGINT NOT NULL, "
            + "`DataChange_CreatedTime` TIMESTAMP NOT NULL)");
    jdbcTemplate.execute(
        "CREATE TABLE `Release` (" + "`Id` BIGINT PRIMARY KEY, `ReleaseKey` VARCHAR(64) NOT NULL, "
            + "`AppId` VARCHAR(64) NOT NULL, `ClusterName` VARCHAR(32) NOT NULL, "
            + "`NamespaceName` VARCHAR(32) NOT NULL, `IsDeleted` BOOLEAN NOT NULL, "
            + "`DeletedAt` BIGINT NOT NULL, `DataChange_LastModifiedBy` VARCHAR(64), "
            + "`DataChange_LastTime` TIMESTAMP NOT NULL, " + "UNIQUE (`ReleaseKey`, `DeletedAt`))");
    jdbcTemplate.execute("CREATE TABLE `ReleaseHistory` ("
        + "`Id` BIGINT PRIMARY KEY, `AppId` VARCHAR(64) NOT NULL, "
        + "`ClusterName` VARCHAR(32) NOT NULL, `NamespaceName` VARCHAR(32) NOT NULL, "
        + "`BranchName` VARCHAR(32) NOT NULL, `ReleaseId` BIGINT NOT NULL, "
        + "`PreviousReleaseId` BIGINT NOT NULL, `IsDeleted` BOOLEAN NOT NULL, "
        + "`DeletedAt` BIGINT NOT NULL, `DataChange_LastTime` TIMESTAMP NOT NULL)");
    jdbcTemplate.execute("CREATE INDEX `IX_ReleaseId` ON `ReleaseHistory` (`ReleaseId`)");
    jdbcTemplate
        .execute("CREATE INDEX `IX_PreviousReleaseId` ON `ReleaseHistory` (`PreviousReleaseId`)");
    jdbcTemplate.execute("CREATE TABLE `GrayReleaseRule` ("
        + "`Id` BIGINT PRIMARY KEY, `ReleaseId` BIGINT NOT NULL, "
        + "`BranchStatus` INTEGER NOT NULL, `IsDeleted` BOOLEAN NOT NULL)");
  }

  private void insertMigrationFixture() {
    insertNamespace(1, "application", false, 0, "2026-01-01 00:00:00");
    insertRelease(1, "release-1", true, 1);
    insertRelease(2, "release-2", true, 2);
    insertRelease(3, "release-3", true, 3);
    insertRelease(4, "release-4", true, 4);
    insertRelease(5, "release-5", true, 5);
    insertRelease(6, "release-6", false, 0);
    insertReleaseHistory(1, 1, 2, false);
    insertReleaseHistory(2, 4, 0, true);
    insertGrayReleaseRule(1, 3, 1);
  }

  private void insertRelease(long id, String releaseKey, boolean deleted, long deletedAt) {
    insertRelease(id, releaseKey, "application", deleted, deletedAt, "2026-02-01 00:00:00");
  }

  private void insertRelease(long id, String releaseKey, String namespaceName, boolean deleted,
      long deletedAt, String lastModifiedTime) {
    jdbcTemplate.update(
        "INSERT INTO `Release` (`Id`, `ReleaseKey`, `AppId`, `ClusterName`, "
            + "`NamespaceName`, `IsDeleted`, `DeletedAt`, `DataChange_LastModifiedBy`, "
            + "`DataChange_LastTime`) " + "VALUES (?, ?, 'app', 'default', ?, ?, ?, '', ?)",
        id, releaseKey, namespaceName, deleted, deletedAt, lastModifiedTime);
  }

  private void insertReleaseHistory(long id, long releaseId, long previousReleaseId,
      boolean deleted) {
    insertReleaseHistory(id, "application", releaseId, previousReleaseId, deleted, deleted ? id : 0,
        "2026-02-01 00:00:00");
  }

  private void insertReleaseHistory(long id, String namespaceName, long releaseId,
      long previousReleaseId, boolean deleted, String lastModifiedTime) {
    insertReleaseHistory(id, namespaceName, releaseId, previousReleaseId, deleted, deleted ? id : 0,
        lastModifiedTime);
  }

  private void insertReleaseHistory(long id, String namespaceName, long releaseId,
      long previousReleaseId, boolean deleted, long deletedAt, String lastModifiedTime) {
    jdbcTemplate.update(
        "INSERT INTO `ReleaseHistory` (`Id`, `AppId`, `ClusterName`, "
            + "`NamespaceName`, `BranchName`, `ReleaseId`, `PreviousReleaseId`, `IsDeleted`, "
            + "`DeletedAt`, `DataChange_LastTime`) "
            + "VALUES (?, 'app', 'default', ?, 'default', ?, ?, ?, ?, ?)",
        id, namespaceName, releaseId, previousReleaseId, deleted, deletedAt, lastModifiedTime);
  }

  private void insertNamespace(long id, String namespaceName, boolean deleted, long deletedAt,
      String createdTime) {
    jdbcTemplate.update(
        "INSERT INTO `Namespace` (`Id`, `AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, "
            + "`DeletedAt`, `DataChange_CreatedTime`) VALUES (?, 'app', 'default', ?, ?, ?, ?)",
        id, namespaceName, deleted, deletedAt, createdTime);
  }

  private void insertGrayReleaseRule(long id, long releaseId, int branchStatus) {
    jdbcTemplate
        .update("INSERT INTO `GrayReleaseRule` (`Id`, `ReleaseId`, `BranchStatus`, `IsDeleted`) "
            + "VALUES (?, ?, ?, FALSE)", id, releaseId, branchStatus);
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
    // H2's MySQL mode does not support MySQL index hints. The raw script hints are asserted
    // separately, while H2 verifies the equivalent query behavior without optimizer directives.
    sql = sql.replaceAll("(?i)\\s+FORCE\\s+INDEX\\s*\\(`[^`]+`\\)", "");
    return Arrays.stream(sql.split(";")).map(String::trim).filter(statement -> !statement.isEmpty())
        .collect(Collectors.toList());
  }

  private void assertReleaseHistoryIndexHints(String scriptName, int expectedPerIndex)
      throws IOException {
    String sql = readScript(scriptName);
    assertEquals(expectedPerIndex, occurrences(sql, "FORCE INDEX (`IX_ReleaseId`)"));
    assertEquals(expectedPerIndex, occurrences(sql, "FORCE INDEX (`IX_PreviousReleaseId`)"));
  }

  private String readScript(String scriptName) throws IOException {
    return Files.readString(scriptDirectory.resolve(scriptName), StandardCharsets.UTF_8);
  }

  private int occurrences(String value, String token) {
    int count = 0;
    int offset = 0;
    while ((offset = value.indexOf(token, offset)) >= 0) {
      count++;
      offset += token.length();
    }
    return count;
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

  private boolean releaseHistoryExists(long releaseHistoryId) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `ReleaseHistory` WHERE `Id` = ?",
        Integer.class, releaseHistoryId) > 0;
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
