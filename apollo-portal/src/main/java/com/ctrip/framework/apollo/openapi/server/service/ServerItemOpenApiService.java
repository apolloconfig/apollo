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
package com.ctrip.framework.apollo.openapi.server.service;

import java.util.Collections;
import java.util.List;

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

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceSyncModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;


/**
 * @author wxq
 */
@Service
public class ServerItemOpenApiService implements ItemOpenApiService {

  private final ItemService itemService;

  public ServerItemOpenApiService(ItemService itemService) {
    this.itemService = itemService;
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
    //protect. only value,type,comment,lastModifiedBy can be modified
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
  public void removeItem(String appId, String env, String clusterName, String namespaceName,
      String key, String operator) {
    ItemDTO toDeleteItem = this.itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    this.itemService.deleteItem(Env.valueOf(env), toDeleteItem.getId(), operator);
  }

  @Override
  public OpenPageDTO<OpenItemDTO> findItemsByNamespace(String appId, String env, String clusterName,
                                                       String namespaceName, int page, int size) {
    PageDTO<OpenItemDTO> commonOpenItemDTOPage =
            this.itemService.findItemsByNamespace(appId, Env.valueOf(env), clusterName, namespaceName, page, size);

    return new OpenPageDTO<>(commonOpenItemDTOPage.getPage(), commonOpenItemDTOPage.getSize(),
            commonOpenItemDTOPage.getTotal(), commonOpenItemDTOPage.getContent());
  }

  /**
   * 通过文本方式批量修改配置项
   */
  public void modifyItemsByText(String appId, String env, String clusterName, String namespaceName,
                               NamespaceTextModel model) {
    model.setAppId(appId);
    model.setClusterName(clusterName);
    model.setEnv(env);
    model.setNamespaceName(namespaceName);

    itemService.updateConfigItemByText(model);
  }

  /**
   * 查找分支下的配置项
   */
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
  public List<ItemDiffs> diff(NamespaceSyncModel model) {
    return itemService.compare(model.getSyncToNamespaces(), model.getSyncItems());
  }

  /**
   * 批量同步配置项到多个命名空间
   */
  public void syncItems(NamespaceSyncModel model) {
    itemService.syncItems(model.getSyncToNamespaces(), model.getSyncItems());
  }

  /**
   * 语法检查
   */
  public void syntaxCheckText(NamespaceTextModel model) {
    doSyntaxCheck(model);
  }

  /**
   * 撤销配置项更改
   */
  public void revokeItems(String appId, String env, String clusterName, String namespaceName) {
    itemService.revokeItem(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  /**
   * 增强版查找配置项，支持排序
   */
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
  private void doSyntaxCheck(NamespaceTextModel model) {
    if (StringUtils.isBlank(model.getConfigText())) {
      return;
    }

    // 只支持yaml语法检查
    if (model.getFormat() != ConfigFileFormat.YAML && model.getFormat() != ConfigFileFormat.YML) {
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
