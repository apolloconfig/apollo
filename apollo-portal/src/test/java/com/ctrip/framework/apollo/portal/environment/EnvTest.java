/*
 * Copyright 2021 Apollo Authors
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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

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
        assertFalse(Env.valueOf(name).equals(Env.DEV));
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
        assertTrue(Env.DEV.equals(Env.DEV));
        assertTrue(Env.DEV.equals(Env.valueOf("dEV")));
        assertFalse(Env.PRO.equals(Env.DEV));
        assertFalse(Env.DEV.equals(Env.valueOf("uaT")));
    }

    @Test
    public void testToString() {
        assertEquals("DEV", Env.DEV.toString());
    }

    @Test
    public void name() {
        assertEquals("DEV", Env.DEV.name());
    }

    @Test
    public void getName() {
        String name = "getName";
        Env.addEnvironment(name);
        assertEquals(name.trim().toUpperCase(), Env.valueOf(name).toString());
    }
}