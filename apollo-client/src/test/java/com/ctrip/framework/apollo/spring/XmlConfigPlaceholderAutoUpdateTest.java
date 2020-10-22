package com.ctrip.framework.apollo.spring;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.internals.SimpleConfig;
import com.ctrip.framework.apollo.spring.XmlConfigPlaceholderTest.TestXmlBean;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.primitives.Ints;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlConfigPlaceholderAutoUpdateTest extends AbstractSpringIntegrationTest {
  private static final String TIMEOUT_PROPERTY = "timeout";
  private static final int DEFAULT_TIMEOUT = 100;
  private static final String BATCH_PROPERTY = "batch";
  private static final int DEFAULT_BATCH = 200;
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";

  @Test
  public void testAutoUpdateWithOneNamespace() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    int newBatch = 2001;

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout),
        BATCH_PROPERTY, String.valueOf(newBatch));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(newBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateDisabled() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    int newBatch = 2001;

    MockConfigUtil mockConfigUtil = new MockConfigUtil();
    mockConfigUtil.setAutoUpdateInjectedSpringProperties(false);

    MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout),
        BATCH_PROPERTY, String.valueOf(newBatch));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithMultipleNamespaces() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    int newBatch = 2001;

    Properties applicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(initialTimeout));
    Properties fxApolloProperties = assembleProperties(BATCH_PROPERTY,
        String.valueOf(initialBatch));

    SimpleConfig applicationConfig = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION,
        applicationProperties);
    SimpleConfig fxApolloConfig = prepareConfig(FX_APOLLO_NAMESPACE, fxApolloProperties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest3.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newApplicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(newTimeout));

    applicationConfig
        .onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newApplicationProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newFxApolloProperties = assembleProperties(BATCH_PROPERTY, String.valueOf(newBatch));

    fxApolloConfig.onRepositoryChange(FX_APOLLO_NAMESPACE, newFxApolloProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(newBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithMultipleNamespacesWithSameProperties() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;
    int anotherBatch = 3000;
    int someNewTimeout = 1001;
    int someNewBatch = 2001;

    Properties applicationProperties = assembleProperties(BATCH_PROPERTY,
        String.valueOf(someBatch));
    Properties fxApolloProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(someTimeout), BATCH_PROPERTY, String.valueOf(anotherBatch));

    prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationProperties);
    SimpleConfig fxApolloConfig = prepareConfig(FX_APOLLO_NAMESPACE, fxApolloProperties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest3.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(someTimeout, bean.getTimeout());
    assertEquals(someBatch, bean.getBatch());

    Properties newFxApolloProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(someNewTimeout), BATCH_PROPERTY, String.valueOf(someNewBatch));

    fxApolloConfig.onRepositoryChange(FX_APOLLO_NAMESPACE, newFxApolloProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(someNewTimeout, bean.getTimeout());
    assertEquals(someBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithNewProperties() throws Exception {
    int initialTimeout = 1000;
    int newTimeout = 1001;
    int newBatch = 2001;

    Properties applicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(initialTimeout));

    SimpleConfig applicationConfig = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION,
        applicationProperties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(DEFAULT_BATCH, bean.getBatch());

    Properties newApplicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(newTimeout), BATCH_PROPERTY, String.valueOf(newBatch));

    applicationConfig
        .onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newApplicationProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(newBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithIrrelevantProperties() throws Exception {
    int initialTimeout = 1000;

    String someIrrelevantKey = "someIrrelevantKey";
    String someIrrelevantValue = "someIrrelevantValue";

    String anotherIrrelevantKey = "anotherIrrelevantKey";
    String anotherIrrelevantValue = "anotherIrrelevantValue";

    Properties applicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(initialTimeout), someIrrelevantKey, someIrrelevantValue);

    SimpleConfig applicationConfig = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION,
        applicationProperties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(DEFAULT_BATCH, bean.getBatch());

    Properties newApplicationProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(initialTimeout), anotherIrrelevantKey, anotherIrrelevantValue);

    applicationConfig
        .onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newApplicationProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(DEFAULT_BATCH, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithDeletedProperties() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = new Properties();

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(DEFAULT_TIMEOUT, bean.getTimeout());
    assertEquals(DEFAULT_BATCH, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithMultipleNamespacesWithSamePropertiesDeleted() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;
    int anotherBatch = 3000;

    Properties applicationProperties = assembleProperties(BATCH_PROPERTY,
        String.valueOf(someBatch));
    Properties fxApolloProperties = assembleProperties(TIMEOUT_PROPERTY,
        String.valueOf(someTimeout), BATCH_PROPERTY, String.valueOf(anotherBatch));

    SimpleConfig applicationConfig = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION,
        applicationProperties);
    prepareConfig(FX_APOLLO_NAMESPACE, fxApolloProperties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest3.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(someTimeout, bean.getTimeout());
    assertEquals(someBatch, bean.getBatch());

    Properties newProperties = new Properties();

    applicationConfig.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(someTimeout, bean.getTimeout());
    assertEquals(anotherBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithDeletedPropertiesWithNoDefaultValue() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest7.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(300);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithTypeMismatch() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    String newBatch = "newBatch";

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest1.xml");

    TestXmlBean bean = context.getBean(TestXmlBean.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout),
        BATCH_PROPERTY, newBatch);

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(300);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithValueInjectedAsConstructorArgs() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    int newBatch = 2001;

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest8.xml");

    TestXmlBeanWithConstructorArgs bean = context.getBean(TestXmlBeanWithConstructorArgs.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout),
        BATCH_PROPERTY, String.valueOf(newBatch));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    // Does not support this scenario
    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithValueAndProperty() throws Exception {
    int initialTimeout = 1000;
    int initialBatch = 2000;
    int newTimeout = 1001;
    int newBatch = 2001;

    Properties properties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(initialTimeout),
        BATCH_PROPERTY, String.valueOf(initialBatch));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest9.xml");

    TestXmlBeanWithInjectedValue bean = context.getBean(TestXmlBeanWithInjectedValue.class);

    assertEquals(initialTimeout, bean.getTimeout());
    assertEquals(initialBatch, bean.getBatch());

    Properties newProperties = assembleProperties(TIMEOUT_PROPERTY, String.valueOf(newTimeout),
        BATCH_PROPERTY, String.valueOf(newBatch));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(newTimeout, bean.getTimeout());
    assertEquals(newBatch, bean.getBatch());
  }

  @Test
  public void testAutoUpdateWithAllKindsOfDataTypes() throws Exception {
    int someInt = 1000;
    int someNewInt = 1001;
    int[] someIntArray = {1, 2, 3, 4};
    int[] someNewIntArray = {5, 6, 7, 8};
    long someLong = 2000L;
    long someNewLong = 2001L;
    short someShort = 3000;
    short someNewShort = 3001;
    float someFloat = 1.2F;
    float someNewFloat = 2.2F;
    double someDouble = 3.10D;
    double someNewDouble = 4.10D;
    byte someByte = 123;
    byte someNewByte = 124;
    boolean someBoolean = true;
    boolean someNewBoolean = !someBoolean;
    String someString = "someString";
    String someNewString = "someNewString";

    String someDateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    Date someDate = assembleDate(2018, 2, 23, 20, 1, 2, 123);
    Date someNewDate = assembleDate(2018, 2, 23, 21, 2, 3, 345);
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(someDateFormat, Locale.US);

    Properties properties = new Properties();
    properties.setProperty("intProperty", String.valueOf(someInt));
    properties.setProperty("intArrayProperty", Ints.join(", ", someIntArray));
    properties.setProperty("longProperty", String.valueOf(someLong));
    properties.setProperty("shortProperty", String.valueOf(someShort));
    properties.setProperty("floatProperty", String.valueOf(someFloat));
    properties.setProperty("doubleProperty", String.valueOf(someDouble));
    properties.setProperty("byteProperty", String.valueOf(someByte));
    properties.setProperty("booleanProperty", String.valueOf(someBoolean));
    properties.setProperty("stringProperty", someString);
    properties.setProperty("dateFormat", someDateFormat);
    properties.setProperty("dateProperty", simpleDateFormat.format(someDate));

    SimpleConfig config = prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/XmlConfigPlaceholderTest10.xml");

    TestAllKindsOfDataTypesBean bean = context.getBean(TestAllKindsOfDataTypesBean.class);

    assertEquals(someInt, bean.getIntProperty());
    assertArrayEquals(someIntArray, bean.getIntArrayProperty());
    assertEquals(someLong, bean.getLongProperty());
    assertEquals(someShort, bean.getShortProperty());
    assertEquals(someFloat, bean.getFloatProperty(), 0.001F);
    assertEquals(someDouble, bean.getDoubleProperty(), 0.001D);
    assertEquals(someByte, bean.getByteProperty());
    assertEquals(someBoolean, bean.getBooleanProperty());
    assertEquals(someString, bean.getStringProperty());
    assertEquals(someDate, bean.getDateProperty());

    Properties newProperties = new Properties();
    newProperties.setProperty("intProperty", String.valueOf(someNewInt));
    newProperties.setProperty("intArrayProperty", Ints.join(", ", someNewIntArray));
    newProperties.setProperty("longProperty", String.valueOf(someNewLong));
    newProperties.setProperty("shortProperty", String.valueOf(someNewShort));
    newProperties.setProperty("floatProperty", String.valueOf(someNewFloat));
    newProperties.setProperty("doubleProperty", String.valueOf(someNewDouble));
    newProperties.setProperty("byteProperty", String.valueOf(someNewByte));
    newProperties.setProperty("booleanProperty", String.valueOf(someNewBoolean));
    newProperties.setProperty("stringProperty", someNewString);
    newProperties.setProperty("dateFormat", someDateFormat);
    newProperties.setProperty("dateProperty", simpleDateFormat.format(someNewDate));

    config.onRepositoryChange(ConfigConsts.NAMESPACE_APPLICATION, newProperties);

    TimeUnit.MILLISECONDS.sleep(100);

    assertEquals(someNewInt, bean.getIntProperty());
    assertArrayEquals(someNewIntArray, bean.getIntArrayProperty());
    assertEquals(someNewLong, bean.getLongProperty());
    assertEquals(someNewShort, bean.getShortProperty());
    assertEquals(someNewFloat, bean.getFloatProperty(), 0.001F);
    assertEquals(someNewDouble, bean.getDoubleProperty(), 0.001D);
    assertEquals(someNewByte, bean.getByteProperty());
    assertEquals(someNewBoolean, bean.getBooleanProperty());
    assertEquals(someNewString, bean.getStringProperty());
    assertEquals(someNewDate, bean.getDateProperty());
  }

  public static class TestXmlBeanWithConstructorArgs {
    private final int timeout;
    private final int batch;

    public TestXmlBeanWithConstructorArgs(int timeout, int batch) {
      this.timeout = timeout;
      this.batch = batch;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getBatch() {
      return batch;
    }
  }

  public static class TestXmlBeanWithInjectedValue {
    @Value("${timeout}")
    private int timeout;
    private int batch;

    public void setBatch(int batch) {
      this.batch = batch;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getBatch() {
      return batch;
    }
  }

  static class TestAllKindsOfDataTypesBean {

    private int intProperty;

    private int[] intArrayProperty;

    private long longProperty;

    private short shortProperty;

    private float floatProperty;

    private double doubleProperty;

    private byte byteProperty;

    private boolean booleanProperty;

    private String stringProperty;

    private Date dateProperty;

    public void setDateProperty(Date dateProperty) {
      this.dateProperty = dateProperty;
    }

    public void setIntProperty(int intProperty) {
      this.intProperty = intProperty;
    }

    public void setIntArrayProperty(int[] intArrayProperty) {
      this.intArrayProperty = intArrayProperty;
    }

    public void setLongProperty(long longProperty) {
      this.longProperty = longProperty;
    }

    public void setShortProperty(short shortProperty) {
      this.shortProperty = shortProperty;
    }

    public void setFloatProperty(float floatProperty) {
      this.floatProperty = floatProperty;
    }

    public void setDoubleProperty(double doubleProperty) {
      this.doubleProperty = doubleProperty;
    }

    public void setByteProperty(byte byteProperty) {
      this.byteProperty = byteProperty;
    }

    public void setBooleanProperty(boolean booleanProperty) {
      this.booleanProperty = booleanProperty;
    }

    public void setStringProperty(String stringProperty) {
      this.stringProperty = stringProperty;
    }

    public int getIntProperty() {
      return intProperty;
    }

    public int[] getIntArrayProperty() {
      return intArrayProperty;
    }

    public long getLongProperty() {
      return longProperty;
    }

    public short getShortProperty() {
      return shortProperty;
    }

    public float getFloatProperty() {
      return floatProperty;
    }

    public double getDoubleProperty() {
      return doubleProperty;
    }

    public byte getByteProperty() {
      return byteProperty;
    }

    public boolean getBooleanProperty() {
      return booleanProperty;
    }

    public String getStringProperty() {
      return stringProperty;
    }

    public Date getDateProperty() {
      return dateProperty;
    }
  }
}
