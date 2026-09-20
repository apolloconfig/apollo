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
package com.ctrip.framework.apollo.openapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.common.controller.HttpMessageConverterConfiguration;
import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.common.dto.InstanceConfigDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.beans.PropertyDescriptor;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/** Verifies populated response fields survive the generated OpenAPI model boundary. */
class OpenApiModelConvertersFieldPreservationTest {

  private static final Gson GSON = new HttpMessageConverterConfiguration().gson();
  private static final Date CREATED = Date.from(Instant.parse("2026-09-19T01:02:03.123Z"));
  private static final Date MODIFIED = Date.from(Instant.parse("2026-09-20T04:05:06.789Z"));

  @ParameterizedTest(name = "{0} preserves legacy audit timestamps")
  @MethodSource("auditConversions")
  void shouldPreserveEveryAuditTimestamp(String name, Object source, Supplier<?> convert) {
    BeanWrapper bean = new BeanWrapperImpl(source);
    List<String> fields = dateProperties(bean);
    assertThat(fields).isNotEmpty();
    fields.forEach(field -> bean.setPropertyValue(field,
        field.equals("dataChangeCreatedTime") ? CREATED : MODIFIED));

    JsonObject expected = json(source);
    JsonObject actual = json(convert.get());
    fields.forEach(field -> assertThat(actual.get(field)).as("%s.%s", name, field)
        .isEqualTo(expected.get(field)));
  }

  @ParameterizedTest(name = "{0} preserves absent audit timestamps")
  @MethodSource("auditConversions")
  void shouldNotInventAuditTimestamps(String name, Object source, Supplier<?> convert) {
    BeanWrapper bean = new BeanWrapperImpl(source);
    List<String> fields = dateProperties(bean);
    fields.forEach(field -> bean.setPropertyValue(field, null));

    JsonObject actual = json(convert.get());
    fields.forEach(field -> assertThat(actual.has(field)).as("%s.%s", name, field).isFalse());
  }

  @Test
  void namespaceAndItemBosShouldUseTheSameTimestampConversions() {
    NamespaceDTO namespace = new NamespaceDTO();
    namespace.setDataChangeCreatedTime(CREATED);
    namespace.setDataChangeLastModifiedTime(MODIFIED);
    ItemDTO item = new ItemDTO("key", "value", "comment", 1);
    item.setDataChangeCreatedTime(CREATED);
    item.setDataChangeLastModifiedTime(MODIFIED);
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(item);
    NamespaceBO namespaceBO = new NamespaceBO();
    namespaceBO.setBaseInfo(namespace);
    namespaceBO.setItems(List.of(itemBO));

    OpenNamespaceDTO result = OpenApiModelConverters.fromNamespaceBO(namespaceBO);

    assertThat(result.getDataChangeCreatedTime())
        .isEqualTo(json(namespace).get("dataChangeCreatedTime").getAsString());
    assertThat(result.getDataChangeLastModifiedTime())
        .isEqualTo(json(namespace).get("dataChangeLastModifiedTime").getAsString());
    assertThat(result.getItems().get(0).getDataChangeCreatedTime())
        .isEqualTo(json(item).get("dataChangeCreatedTime").getAsString());
    assertThat(result.getItems().get(0).getDataChangeLastModifiedTime())
        .isEqualTo(json(item).get("dataChangeLastModifiedTime").getAsString());
  }

  @Test
  void grayRulesShouldPreserveAllTargetsWhenReadAndUpdated() {
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO("app", "default", "application", "gray");
    GrayReleaseRuleItemDTO first =
        new GrayReleaseRuleItemDTO("client-a", new LinkedHashSet<>(List.of("10.0.0.1", "10.0.0.2")),
            new LinkedHashSet<>(List.of("blue", "canary")));
    GrayReleaseRuleItemDTO second = new GrayReleaseRuleItemDTO("client-b",
        new LinkedHashSet<>(Set.of("*")), new LinkedHashSet<>(Set.of("*")));
    rules.setRuleItems(new LinkedHashSet<>(List.of(first, second)));

    OpenGrayReleaseRuleDTO result = OpenApiModelConverters.fromGrayReleaseRuleDTO(rules);

    assertThat(json(result).get("ruleItems")).isEqualTo(json(rules).get("ruleItems"));
    result.addRuleItemsItem(new OpenGrayReleaseRuleItemDTO().clientAppId("client-c")
        .clientIpList(Set.of("10.0.0.3")).clientLabelList(Set.of("green")));
    GrayReleaseRuleDTO update = OpenApiModelConverters.toGrayReleaseRuleDTO(result);
    assertThat(update.getRuleItems()).extracting(GrayReleaseRuleItemDTO::getClientAppId)
        .containsExactlyInAnyOrder("client-a", "client-b", "client-c");
    GrayReleaseRuleItemDTO retained = update.getRuleItems().stream()
        .filter(rule -> rule.getClientAppId().equals("client-a")).findFirst().orElseThrow();
    assertThat(retained.getClientIpList()).containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");
    assertThat(retained.getClientLabelList()).containsExactlyInAnyOrder("blue", "canary");

    OpenGrayReleaseRuleItemDTO copied = result.getRuleItems().iterator().next();
    copied.getClientIpList().add("10.0.0.9");
    copied.getClientLabelList().add("new-label");
    assertThat(first.getClientIpList()).containsExactly("10.0.0.1", "10.0.0.2");
    assertThat(first.getClientLabelList()).containsExactly("blue", "canary");
    assertThat(rules.getRuleItems()).hasSize(2);
  }

  @Test
  void grayRulesShouldSupportMissingTargetCollections() {
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO("app", "default", "application", "gray");
    rules.addRuleItem(new GrayReleaseRuleItemDTO("client", null, null));
    OpenGrayReleaseRuleDTO result = OpenApiModelConverters.fromGrayReleaseRuleDTO(rules);
    assertThat(result.getRuleItems()).singleElement().satisfies(rule -> {
      assertThat(rule.getClientIpList()).isEmpty();
      assertThat(rule.getClientLabelList()).isEmpty();
    });
    rules.setRuleItems(null);
    assertThat(OpenApiModelConverters.fromGrayReleaseRuleDTO(rules).getRuleItems()).isEmpty();
  }

  @Test
  void instancesShouldPreserveNestedReleasesAndDeliveryTimes() {
    InstanceDTO instance = new InstanceDTO();
    instance.setId(1L);
    instance.setAppId("client-app");
    instance.setIp("10.0.0.1");
    instance.setDataChangeCreatedTime(CREATED);
    InstanceConfigDTO first = instanceConfig(10L, "old", "100");
    InstanceConfigDTO second = instanceConfig(11L, "latest", "200");
    instance.setConfigs(List.of(first, second));

    OpenInstanceDTO result = OpenApiModelConverters.fromInstanceDTO(instance);

    assertThat(result.getConfigs()).hasSize(2);
    for (int index = 0; index < instance.getConfigs().size(); index++) {
      InstanceConfigDTO source = instance.getConfigs().get(index);
      JsonObject expected = json(source);
      JsonObject actual = json(result.getConfigs().get(index));
      assertThat(actual.get("releaseDeliveryTime")).isEqualTo(expected.get("releaseDeliveryTime"));
      assertThat(actual.get("dataChangeLastModifiedTime"))
          .isEqualTo(expected.get("dataChangeLastModifiedTime"));
      assertThat(actual.getAsJsonObject("release").get("dataChangeCreatedTime"))
          .isEqualTo(expected.getAsJsonObject("release").get("dataChangeCreatedTime"));
      assertThat(result.getConfigs().get(index).getRelease().getId())
          .isEqualTo(source.getRelease().getId());
      assertThat(result.getConfigs().get(index).getRelease().getName())
          .isEqualTo(source.getRelease().getName());
      assertThat(result.getConfigs().get(index).getRelease().getConfigurations())
          .containsEntry("timeout", index == 0 ? "100" : "200");
    }
    result.getConfigs().get(0).getRelease().setName("changed");
    assertThat(first.getRelease().getName()).isEqualTo("old");
  }

  @Test
  void instancesShouldSupportMissingReleasesAndEmptyConfigs() {
    InstanceDTO instance = new InstanceDTO();
    instance.setConfigs(List.of(new InstanceConfigDTO()));
    assertThat(OpenApiModelConverters.fromInstanceDTO(instance).getConfigs()).singleElement()
        .satisfies(config -> {
          assertThat(config.getRelease()).isNull();
          assertThat(config.getReleaseDeliveryTime()).isNull();
          assertThat(config.getDataChangeLastModifiedTime()).isNull();
        });
    instance.setConfigs(null);
    assertThat(OpenApiModelConverters.fromInstanceDTO(instance).getConfigs()).isEmpty();
  }

  private static Stream<Arguments> auditConversions() {
    return Stream.of(auditCase("Item", new ItemDTO(), OpenApiModelConverters::fromItemDTO),
        auditCase("Namespace", new NamespaceDTO(), OpenApiModelConverters::fromNamespaceDTO),
        auditCase("App", new App(), OpenApiModelConverters::fromApp),
        auditCase("AppNamespace", new AppNamespace(), OpenApiModelConverters::fromAppNamespace),
        auditCase("Cluster", new ClusterDTO(), OpenApiModelConverters::fromClusterDTO),
        auditCase("Release", new ReleaseDTO(), OpenApiModelConverters::fromReleaseDTO),
        auditCase("AccessKey", new AccessKeyDTO(), OpenApiModelConverters::fromAccessKeyDTO),
        auditCase("GrayReleaseRule",
            new GrayReleaseRuleDTO("app", "default", "application", "gray"),
            OpenApiModelConverters::fromGrayReleaseRuleDTO),
        auditCase("Instance", new InstanceDTO(), OpenApiModelConverters::fromInstanceDTO));
  }

  private static <T> Arguments auditCase(String name, T source, Function<T, ?> convert) {
    return Arguments.of(name, source, (Supplier<?>) () -> convert.apply(source));
  }

  private static List<String> dateProperties(BeanWrapper bean) {
    return Arrays.stream(bean.getPropertyDescriptors())
        .filter(property -> property.getPropertyType() == Date.class)
        .map(PropertyDescriptor::getName).toList();
  }

  private static InstanceConfigDTO instanceConfig(long id, String name, String value) {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(id);
    release.setName(name);
    release.setConfigurations("{\"timeout\":\"" + value + "\"}");
    release.setDataChangeCreatedTime(CREATED);
    release.setDataChangeLastModifiedTime(MODIFIED);
    InstanceConfigDTO config = new InstanceConfigDTO();
    config.setRelease(release);
    config.setReleaseDeliveryTime(CREATED);
    config.setDataChangeLastModifiedTime(MODIFIED);
    return config;
  }

  private static JsonObject json(Object value) {
    return GSON.toJsonTree(value).getAsJsonObject();
  }

}
