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
package com.ctrip.framework.test.tools;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/21
 */
public class AloneRunner extends Runner {

  private static final String CONSTRUCTOR_ERROR_FORMAT = "Custom runner class %s should have a public constructor with signature %s(Class testClass)";
  private final Runner realRunner;

  private final ClassLoader testCaseClassloader;

  public AloneRunner(Class<?> clazz) throws InitializationError {

    AloneWith annotation = clazz.getAnnotation(
        AloneWith.class);

    Class<? extends Runner> realClassRunnerClass =
        annotation == null ? JUnit4.class : annotation.value();
    if (AloneRunner.class.isAssignableFrom(realClassRunnerClass)) {
      throw new InitializationError("Dead-loop code");
    }

    testCaseClassloader = new AloneClassLoader();
    ClassLoader backupClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(testCaseClassloader);
    try {
      Class<?> newTestCaseClass = testCaseClassloader.loadClass(clazz.getName());
      Class<? extends Runner> realRunnerClass = (Class<? extends Runner>) testCaseClassloader
          .loadClass(realClassRunnerClass.getName());
      realRunner = buildRunner(realRunnerClass, newTestCaseClass);
    } catch (ReflectiveOperationException e) {
      throw new InitializationError(e);
    } finally {
      Thread.currentThread().setContextClassLoader(backupClassLoader);
    }
  }

  public Description getDescription() {
    return realRunner.getDescription();
  }

  public void run(RunNotifier runNotifier) {
    ClassLoader backupClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(testCaseClassloader);

    realRunner.run(runNotifier);

    Thread.currentThread().setContextClassLoader(backupClassLoader);
  }

  protected Runner buildRunner(Class<? extends Runner> runnerClass,
      Class<?> testClass) throws ReflectiveOperationException, InitializationError {
    try {
      return runnerClass.getConstructor(Class.class).newInstance(testClass);
    } catch (NoSuchMethodException e) {
      String simpleName = runnerClass.getSimpleName();
      throw new InitializationError(String.format(
          CONSTRUCTOR_ERROR_FORMAT, simpleName, simpleName));
    }
  }
}