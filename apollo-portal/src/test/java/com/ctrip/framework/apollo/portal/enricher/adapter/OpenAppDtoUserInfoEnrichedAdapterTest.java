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
package com.ctrip.framework.apollo.portal.enricher.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import org.junit.jupiter.api.Test;

/** Verifies app audit identities and the owner are enriched independently. */
class OpenAppDtoUserInfoEnrichedAdapterTest {

  @Test
  void shouldEnrichCreatorModifierAndOwner() {
    OpenAppDTO app = new OpenAppDTO();
    app.setDataChangeCreatedBy("creator");
    app.setDataChangeLastModifiedBy("modifier");
    app.setOwnerName("owner");
    UserInfoEnrichedAdapter adapter = new OpenAppDtoUserInfoEnrichedAdapter(app);

    assertThat(adapter.getFirstUserId()).isEqualTo("creator");
    assertThat(adapter.getSecondUserId()).isEqualTo("modifier");
    assertThat(adapter.getThirdUserId()).isEqualTo("owner");
    adapter.setFirstUserDisplayName("Creator Name");
    adapter.setSecondUserDisplayName("Modifier Name");
    adapter.setThirdUserDisplayName("Owner Name");

    assertThat(app.getDataChangeCreatedByDisplayName()).isEqualTo("Creator Name");
    assertThat(app.getDataChangeLastModifiedByDisplayName()).isEqualTo("Modifier Name");
    assertThat(app.getOwnerDisplayName()).isEqualTo("Owner Name");
  }
}
