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
package com.ctrip.framework.apollo.common.conditional;

import com.ctrip.framework.apollo.common.condition.ConditionalOnMissingProfile;
import com.ctrip.framework.apollo.common.condition.ConditionalOnProfile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.ctrip.framework.apollo.common.conditional.ConditionalOnProfileTest.ANOTHER_PROFILE;
import static com.ctrip.framework.apollo.common.conditional.ConditionalOnProfileTest.SOME_PROFILE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ConditionalOnProfileTest.TestConfiguration.class)
@ActiveProfiles({SOME_PROFILE, ANOTHER_PROFILE})
public class ConditionalOnProfileTest {
  static final String SOME_PROFILE = "someProfile";
  static final String ANOTHER_PROFILE = "anotherProfile";
  static final String YET_ANOTHER_PROFILE = "yetAnotherProfile";

  static boolean someConfigurationEnabled = false;
  static boolean anotherConfigurationEnabled = false;
  static boolean yetAnotherConfigurationEnabled = false;
  static boolean combinedConfigurationEnabled = false;
  static boolean anotherCombinedConfigurationEnabled = false;

  @Test
  public void test() throws Exception {
    assertTrue(someConfigurationEnabled);
    assertFalse(anotherConfigurationEnabled);
    assertTrue(yetAnotherConfigurationEnabled);
    assertTrue(combinedConfigurationEnabled);
    assertFalse(anotherCombinedConfigurationEnabled);
  }

  @Configuration
  static class TestConfiguration {

    @Configuration
    @ConditionalOnProfile(SOME_PROFILE)
    static class SomeConfiguration {
      {
        someConfigurationEnabled = true;
      }
    }

    @Configuration
    @ConditionalOnMissingProfile(ANOTHER_PROFILE)
    static class AnotherConfiguration {
      {
        anotherConfigurationEnabled = true;
      }
    }


    @Configuration
    @ConditionalOnMissingProfile(YET_ANOTHER_PROFILE)
    static class YetAnotherConfiguration {
      {
        yetAnotherConfigurationEnabled = true;
      }
    }

    @Configuration
    @ConditionalOnProfile({SOME_PROFILE, ANOTHER_PROFILE})
    @ConditionalOnMissingProfile(YET_ANOTHER_PROFILE)
    static class CombinedConfiguration {
      {
        combinedConfigurationEnabled = true;
      }
    }

    @Configuration
    @ConditionalOnProfile(SOME_PROFILE)
    @ConditionalOnMissingProfile({YET_ANOTHER_PROFILE, ANOTHER_PROFILE})
    static class AnotherCombinedConfiguration {
      {
        anotherCombinedConfigurationEnabled = true;
      }
    }

  }
}
