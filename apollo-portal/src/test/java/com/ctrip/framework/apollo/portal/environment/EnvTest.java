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
package com.ctrip.framework.apollo.portal.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.junit.Test;

public class EnvTest {

  @Test
  public void exist() {
    assertFalse(Env.exists("xxxyyy234"));
    assertTrue(Env.exists("local"));
    assertTrue(Env.exists("dev"));
  }

  @Test
  public void addEnv() {
    String name = "someEEEE";
    assertFalse(Env.exists(name));
    Env.addEnvironment(name);
    assertTrue(Env.exists(name));
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf() {
    String name = "notexist";
    assertFalse(Env.exists(name));
    assertEquals(Env.valueOf(name), Env.UNKNOWN);
    assertEquals(Env.valueOf("dev"), Env.DEV);
    assertEquals(Env.valueOf("UAT"), Env.UAT);
  }

  @Test
  public void testEquals() {
    assertEquals(Env.DEV, Env.valueOf("dEv"));
    String name = "someEEEE";
    Env.addEnvironment(name);
    assertNotEquals(Env.valueOf(name), Env.DEV);
  }

  @Test(expected = RuntimeException.class)
  public void testEqualsWithRuntimeException()
      throws NoSuchMethodException,
      IllegalAccessException,
      InvocationTargetException,
      InstantiationException {
    // get private constructor
    Constructor<Env> envConstructor = Env.class.getDeclaredConstructor(String.class);
    // make private constructor accessible
    envConstructor.setAccessible(true);
    // make a fake Env
    Env fakeDevEnv = envConstructor.newInstance(Env.DEV.toString());
    // compare, then a RuntimeException will invoke
    fakeDevEnv.equals(Env.DEV);
  }

  @Test
  public void testEqualWithoutException() {
    assertEquals(Env.DEV, Env.DEV);
    assertEquals(Env.DEV, Env.valueOf("dEV"));
    assertNotEquals(Env.PRO, Env.DEV);
    assertNotEquals(Env.DEV, Env.valueOf("uaT"));
  }

  @Test
  public void testToString() {
    assertEquals("DEV", Env.DEV.toString());
  }

  @Test
  public void name() {
    assertEquals("DEV", Env.DEV.getName());
  }

  @Test
  public void getName() {
    String name = "getName";
    Env.addEnvironment(name);
    assertEquals(name.trim().toUpperCase(), Env.valueOf(name).toString());
  }

  @Test
  public void testTransformEnvBlank() {
    assertEquals(Env.UNKNOWN, Env.transformEnv(""));
    assertEquals(Env.UNKNOWN, Env.transformEnv(null));
    assertEquals(Env.UNKNOWN, Env.transformEnv("   "));
  }

  @Test
  public void testTransformEnvSpecialCase() {
    // Prod/Pro
    assertEquals(Env.PRO, Env.transformEnv("prod"));
    assertEquals(Env.PRO, Env.transformEnv("PROD"));

    //FAT/FWS
    assertEquals(Env.FAT, Env.transformEnv("FWS"));
    assertEquals(Env.FAT, Env.transformEnv("fws"));
  }

  @Test
  public void testTransformEnvNotExist() {
    assertEquals(Env.UNKNOWN, Env.transformEnv("notexisting"));
    assertEquals(Env.LOCAL, Env.transformEnv("LOCAL"));
  }

  @Test
  public void testTransformEnvValid() {
    assertEquals(Env.UNKNOWN, Env.transformEnv("UNKNOWN"));
    assertEquals(Env.LOCAL, Env.transformEnv("LOCAL"));
    assertEquals(Env.FAT, Env.transformEnv("FAT"));
    assertEquals(Env.FAT, Env.transformEnv("FWS"));
    assertEquals(Env.PRO, Env.transformEnv("PRO"));
    assertEquals(Env.PRO, Env.transformEnv("PROD"));
    assertEquals(Env.DEV, Env.transformEnv("DEV"));
    assertEquals(Env.LPT, Env.transformEnv("LPT"));
    assertEquals(Env.TOOLS, Env.transformEnv("TOOLS"));
    assertEquals(Env.UAT, Env.transformEnv("UAT"));

    String testEnvName = "testEnv";

    Env.addEnvironment(testEnvName);
    Env expected = Env.valueOf(testEnvName);

    assertEquals(expected, Env.transformEnv(testEnvName));
  }

  @Test
  public void testTransformEnvWithTrailingAndLeadingBlankValid() {
    ArrayList<String> specialChars = new ArrayList<>();
    specialChars.add(" ");
    specialChars.add("\t");
    specialChars.add(" \t");
    specialChars.add(" \t ");
    specialChars.add("\t ");

    for (String specialChar : specialChars) {
      assertEquals(Env.UNKNOWN, Env.transformEnv(specialChar + "UNKNOWN"));
      assertEquals(Env.LOCAL, Env.transformEnv(specialChar + "LOCAL" + specialChar));
      assertEquals(Env.FAT, Env.transformEnv(specialChar + "FAT" + specialChar));
      assertEquals(Env.FAT, Env.transformEnv(specialChar + "FWS" + specialChar));
      assertEquals(Env.PRO, Env.transformEnv(specialChar + "PRO" + specialChar));
      assertEquals(Env.PRO, Env.transformEnv(specialChar + "PROD" + specialChar));
      assertEquals(Env.DEV, Env.transformEnv(specialChar + "DEV" + specialChar));
      assertEquals(Env.LPT, Env.transformEnv(specialChar + "LPT" + specialChar));
      assertEquals(Env.TOOLS, Env.transformEnv(specialChar + "TOOLS" + specialChar));
      assertEquals(Env.UAT, Env.transformEnv(specialChar + "UAT" + specialChar));

      String testEnvName = "testEnv";
      Env.addEnvironment(testEnvName);
      Env expected = Env.valueOf(testEnvName);
      assertEquals(expected, Env.transformEnv(specialChar + testEnvName + specialChar));

    }
  }

  @Test(expected = RuntimeException.class)
  public void testAddEnvironmentBlankString() {
    Env.addEnvironment("");
  }

  @Test(expected = RuntimeException.class)
  public void testAddEnvironmentNullString() {
    Env.addEnvironment(null);
  }

  @Test(expected = RuntimeException.class)
  public void testAddEnvironmentSpacesString() {
    Env.addEnvironment("    ");
  }

  @Test
  public void testExistsForBlankName() {
    assertFalse(Env.exists(""));
    assertFalse(Env.exists("   "));
    assertFalse(Env.exists(null));
  }
}
