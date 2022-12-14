package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import com.ctrip.framework.apollo.biz.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * @author kl (http://kailing.pub)
 * @since 2022/12/13
 */
@Service
public class ServerConfigService {

  private final ServerConfigRepository serverConfigRepository;

  public ServerConfigService(ServerConfigRepository serverConfigRepository) {
    this.serverConfigRepository = serverConfigRepository;
  }

  public List<ServerConfig> findAll() {
    Iterable<ServerConfig> serverConfigs = serverConfigRepository.findAll();
    return Lists.newArrayList(serverConfigs);
  }

  @Transactional
  public ServerConfig createOrUpdateConfig(ServerConfig serverConfig) {

    ServerConfig storedConfig = serverConfigRepository.findByKey(serverConfig.getKey());

    if (Objects.isNull(storedConfig)) {//create
      serverConfig.setId(0L);//为空，设置ID 为0，jpa执行新增操作
      if(Objects.isNull(serverConfig.getCluster())){
        serverConfig.setCluster("default");
      }
      return serverConfigRepository.save(serverConfig);
    }

    //update
    storedConfig.setComment(serverConfig.getComment());
    storedConfig.setDataChangeLastModifiedBy(serverConfig.getDataChangeLastModifiedBy());
    storedConfig.setValue(serverConfig.getValue());

    return serverConfigRepository.save(storedConfig);
  }

}
