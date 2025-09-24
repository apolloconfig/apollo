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
package com.ctrip.framework.apollo.openapi.server.service.impl;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.model.*;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceSyncModel;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * @author wxq
 */
@Service
public class ServerItemOpenApiService implements ItemOpenApiService {

  private final ItemService itemService;
  private final UserInfoHolder userInfoHolder;

  public ServerItemOpenApiService(ItemService itemService, UserInfoHolder userInfoHolder) {
    this.itemService = itemService;
    this.userInfoHolder = userInfoHolder;
  }

  @Override
  public OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName,
      String key) {
    ItemDTO itemDTO = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    return itemDTO == null ? null : OpenApiBeanUtils.transformFromItemDTO(itemDTO);
  }

  @Override
  public OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {

    ItemDTO toCreate = OpenApiBeanUtils.transformToItemDTO(itemDTO);

    //protect
    toCreate.setLineNum(0);
    toCreate.setId(0);
    toCreate.setDataChangeLastModifiedBy(toCreate.getDataChangeCreatedBy());
    toCreate.setDataChangeLastModifiedTime(null);
    toCreate.setDataChangeCreatedTime(null);

    ItemDTO createdItem = itemService.createItem(appId, Env.valueOf(env),
        clusterName, namespaceName, toCreate);
    return OpenApiBeanUtils.transformFromItemDTO(createdItem);
  }

  @Override
  public void updateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {
    ItemDTO toUpdateItem = itemService
        .loadItem(Env.valueOf(env), appId, clusterName, namespaceName, itemDTO.getKey());
    toUpdateItem.setComment(itemDTO.getComment());
    toUpdateItem.setType(itemDTO.getType());
    toUpdateItem.setValue(itemDTO.getValue());
    toUpdateItem.setDataChangeLastModifiedBy(itemDTO.getDataChangeLastModifiedBy());

    itemService.updateItem(appId, Env.valueOf(env), clusterName, namespaceName, toUpdateItem);
  }

  @Override
  public void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {
    try {
      this.updateItem(appId, env, clusterName, namespaceName, itemDTO);
    } catch (Throwable ex) {
      if (ex instanceof HttpStatusCodeException) {
        // check createIfNotExists
        if (((HttpStatusCodeException) ex).getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          this.createItem(appId, env, clusterName, namespaceName, itemDTO);
          return;
        }
      }
      throw ex;
    }
  }

  @Override
  public void removeItem(String appId, String env, String clusterName, String namespaceName, String key) {
    ItemDTO toDeleteItem = this.itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    this.itemService.deleteItem(Env.valueOf(env), toDeleteItem.getId(), userInfoHolder.getUser().getUserId());
  }

  @Override
  public OpenPageDTOOpenItemDTO findItemsByNamespace(String appId, String env, String clusterName,
                                                     String namespaceName, int page, int size) {
    PageDTO<com.ctrip.framework.apollo.openapi.dto.OpenItemDTO> commonOpenItemDTOPage =
            this.itemService.findItemsByNamespace(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
    int pageResult = commonOpenItemDTOPage.getPage();
    int sizeResult = commonOpenItemDTOPage.getSize();
    long totalResult = commonOpenItemDTOPage.getTotal();
    List<OpenItemDTO> openItemDTOContentResult = getOpenItemDTOS(commonOpenItemDTOPage);

    OpenPageDTOOpenItemDTO result = new OpenPageDTOOpenItemDTO();
    result.setPage(pageResult);
    result.setSize(sizeResult);
    result.setTotal(totalResult);
    result.setContent(openItemDTOContentResult);
    return result;
  }

  private static List<OpenItemDTO> getOpenItemDTOS(PageDTO<com.ctrip.framework.apollo.openapi.dto.OpenItemDTO> commonOpenItemDTOPage) {
    List<com.ctrip.framework.apollo.openapi.dto.OpenItemDTO> contentResult = commonOpenItemDTOPage.getContent();
    // 将contentResult转换为yaml generate OpenItemDTO
    List<OpenItemDTO> openItemDTOContentResult = new ArrayList<>();
    if (contentResult != null && !contentResult.isEmpty()) {
      for (com.ctrip.framework.apollo.openapi.dto.OpenItemDTO item : contentResult) {
        OpenItemDTO openItemDTO = new OpenItemDTO();
        openItemDTO.setKey(item.getKey());
        openItemDTO.setValue(item.getValue());
        openItemDTO.setComment(item.getComment());
        openItemDTO.setType(item.getType());
        openItemDTO.setDataChangeCreatedBy(item.getDataChangeCreatedBy());
        openItemDTO.setDataChangeLastModifiedBy(item.getDataChangeLastModifiedBy());
        openItemDTOContentResult.add(openItemDTO);
      }
    }
    return openItemDTOContentResult;
  }

  @Override
  public OpenItemDTO loadItem(Env env, String appId, String clusterName, String namespaceName, String key) {
    ItemDTO itemDTO = itemService.loadItem(env, appId, clusterName, namespaceName, key);
    return itemDTO == null ? null : OpenApiBeanUtils.transformFromItemDTO(itemDTO);
  }

  /**
   * 通过文本方式批量修改配置项
   */
  @Override
  public void modifyItemsByText(String appId, String env, String clusterName, String namespaceName,
                                OpenNamespaceTextModel model) {
    String operator = userInfoHolder.getUser().getUserId();
    model.setOperator(operator);
    model.setAppId(appId);
    model.setClusterName(clusterName);
    model.setEnv(env);
    model.setNamespaceName(namespaceName);

    itemService.updateConfigItemByText(OpenApiBeanUtils.transformToNamespaceTextModel(model));
  }

  /**
   * 查找分支下的配置项
   */
  @Override
  public List<OpenItemDTO> findBranchItems(String appId, String env, String clusterName,
                                           String namespaceName, String branchName) {
    List<ItemDTO> items = itemService.findItems(appId, Env.valueOf(env), branchName, namespaceName);
    if (items == null) {
      return Collections.emptyList();
    }
    
    // 按最后修改时间排序
    items.sort((o1, o2) -> {
      if (o1.getDataChangeLastModifiedTime().after(o2.getDataChangeLastModifiedTime())) {
        return -1;
      }
      if (o1.getDataChangeLastModifiedTime().before(o2.getDataChangeLastModifiedTime())) {
        return 1;
      }
      return 0;
    });
    
    return OpenApiBeanUtils.transformFromItemDTOs(items);
  }

  /**
   * 命名空间差异对比
   */
  @Override
  public List<OpenItemDiffs> diff(OpenNamespaceSyncModel model) {
    NamespaceSyncModel namespaceSyncModel = OpenApiBeanUtils.transformToNamespaceSyncModel(model);
    return OpenApiBeanUtils.transformFromItemDiffsList(
            itemService.compare(namespaceSyncModel.getSyncToNamespaces(), namespaceSyncModel.getSyncItems()));
  }

  /**
   * 批量同步配置项到多个命名空间
   */
  @Override
  public void syncItems(OpenNamespaceSyncModel model) {
    NamespaceSyncModel namespaceSyncModel = OpenApiBeanUtils.transformToNamespaceSyncModel(model);
    itemService.syncItems(namespaceSyncModel.getSyncToNamespaces(), namespaceSyncModel.getSyncItems());
  }

  /**
   * 语法检查
   */
  @Override
  public void syntaxCheckText(OpenNamespaceTextModel model) {
    doSyntaxCheck(model);
  }

  /**
   * 撤销配置项更改
   */
  @Override
  public void revokeItems(String appId, String env, String clusterName, String namespaceName) {
    itemService.revokeItem(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  /**
   * 增强版查找配置项，支持排序
   */
  @Override
  public List<OpenItemDTO> findItemsWithOrder(String appId, String env, String clusterName,
                                              String namespaceName, String orderBy) {
    List<ItemDTO> items = itemService.findItems(appId, Env.valueOf(env), clusterName, namespaceName);
    if (items == null) {
      return Collections.emptyList();
    }
    
    if ("lastModifiedTime".equals(orderBy)) {
      items.sort((o1, o2) -> {
        if (o1.getDataChangeLastModifiedTime().after(o2.getDataChangeLastModifiedTime())) {
          return -1;
        }
        if (o1.getDataChangeLastModifiedTime().before(o2.getDataChangeLastModifiedTime())) {
          return 1;
        }
        return 0;
      });
    }
    
    return OpenApiBeanUtils.transformFromItemDTOs(items);
  }

  /**
   * 语法检查实现
   */
  private void doSyntaxCheck(OpenNamespaceTextModel model) {
    if (StringUtils.isBlank(model.getConfigText())) {
      return;
    }

    // 只支持yaml语法检查
    if (!Objects.equals(model.getFormat(), ConfigFileFormat.YAML.getValue()) && !Objects.equals(model.getFormat(), ConfigFileFormat.YML.getValue())) {
      return;
    }

    // 使用YamlPropertiesFactoryBean检查yaml语法
    TypeLimitedYamlPropertiesFactoryBean yamlPropertiesFactoryBean = new TypeLimitedYamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean.setResources(new ByteArrayResource(model.getConfigText().getBytes()));
    try {
      // 此调用将yaml转换为properties，如果转换失败将抛出异常
      yamlPropertiesFactoryBean.getObject();
    } catch (Exception ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  /**
   * 类型限制的YamlPropertiesFactoryBean
   */
  private static class TypeLimitedYamlPropertiesFactoryBean extends YamlPropertiesFactoryBean {
    @Override
    protected Yaml createYaml() {
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setAllowDuplicateKeys(false);
      DumperOptions dumperOptions = new DumperOptions();
      return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
    }
  }
}
