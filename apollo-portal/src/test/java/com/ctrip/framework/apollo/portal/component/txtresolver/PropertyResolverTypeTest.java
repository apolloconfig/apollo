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
package com.ctrip.framework.apollo.portal.component.txtresolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Regression tests for typed properties edited as text. */
class PropertyResolverTypeTest {
  private final PropertyResolver resolver = new PropertyResolver();

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void preservesTypeWhenChangingValue(int type) {
    ItemDTO original = item(type);
    ItemChangeSets changes =
        resolver.resolve(7, "key=new-value", Collections.singletonList(original));
    assertThat(changes.getUpdateItems()).hasSize(1);
    ItemDTO updated = changes.getUpdateItems().get(0);
    assertThat(updated.getType()).isEqualTo(type);
    assertThat(updated.getId()).isEqualTo(11);
    assertThat(updated.getNamespaceId()).isEqualTo(7);
    assertThat(updated.getValue()).isEqualTo("new-value");
    assertThat(updated.getComment()).isEqualTo("original comment");
    assertThat(original.getValue()).isEqualTo("old-value");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void preservesTypeWhenInsertingLinesBeforeExistingItem(int type) {
    ItemChangeSets changes = resolver.resolve(7, "# heading\nadded=value\nkey=old-value",
        Collections.singletonList(item(type)));
    assertThat(changes.getUpdateItems()).hasSize(1);
    assertThat(changes.getUpdateItems().get(0).getType()).isEqualTo(type);
    assertThat(changes.getUpdateItems().get(0).getLineNum()).isEqualTo(3);
    assertThat(changes.getCreateItems()).allMatch(created -> created.getType() == 0);
  }

  private ItemDTO item(int type) {
    ItemDTO item = new ItemDTO("key", "old-value", "original comment", 1);
    item.setId(11);
    item.setNamespaceId(7);
    item.setType(type);
    return item;
  }
}
