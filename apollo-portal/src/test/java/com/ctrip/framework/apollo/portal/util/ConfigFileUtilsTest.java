package com.ctrip.framework.apollo.portal.util;

import static org.junit.Assert.*;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFileUtilsTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void checkFormat() {
    ConfigFileUtils.checkFormat("1234+default+app.properties");
    ConfigFileUtils.checkFormat("1234+default+app.yml");
    ConfigFileUtils.checkFormat("1234+default+app.json");
  }

  @Test(expected = BadRequestException.class)
  public void checkFormatWithException0() {
    ConfigFileUtils.checkFormat("1234+defaultes");
  }

  @Test(expected = BadRequestException.class)
  public void checkFormatWithException1() {
    ConfigFileUtils.checkFormat(".json");
  }

  @Test(expected = BadRequestException.class)
  public void checkFormatWithException2() {
    ConfigFileUtils.checkFormat("application.");
  }

  @Test
  public void getFormat() {
    final String properties = ConfigFileUtils.getFormat("application+default+application.properties");
    assertEquals("properties", properties);

    final String yml = ConfigFileUtils.getFormat("application+default+application.yml");
    assertEquals("yml", yml);
  }

  @Test
  public void getAppId() {
    final String application = ConfigFileUtils.getAppId("application+default+application.properties");
    assertEquals("application", application);

    final String abc = ConfigFileUtils.getAppId("abc+default+application.yml");
    assertEquals("abc", abc);
  }

  @Test
  public void getClusterName() {
    final String cluster = ConfigFileUtils.getClusterName("application+default+application.properties");
    assertEquals("default", cluster);

    final String Beijing = ConfigFileUtils.getClusterName("abc+Beijing+application.yml");
    assertEquals("Beijing", Beijing);
  }

  @Test
  public void getNamespace() {
    final String application = ConfigFileUtils.getNamespace("234+default+application.properties");
    assertEquals("application", application);

    final String applicationYml = ConfigFileUtils.getNamespace("abc+default+application.yml");
    assertEquals("application.yml", applicationYml);
  }

  @Test
  public void toFilename() {
    final String propertiesFilename0 = ConfigFileUtils.toFilename("123", "default", "application", ConfigFileFormat.Properties);
    logger.info("propertiesFilename0 {}", propertiesFilename0);
    assertEquals("123+default+application.properties", propertiesFilename0);

    final String ymlFilename0 = ConfigFileUtils.toFilename("666", "none", "cc.yml", ConfigFileFormat.YML);
    logger.info("ymlFilename0 {}", ymlFilename0);
    assertEquals("666+none+cc.yml", ymlFilename0);
  }

}