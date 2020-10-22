package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.repository.CommitRepository;
import java.util.Date;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommitService {

  private final CommitRepository commitRepository;

  public CommitService(final CommitRepository commitRepository) {
    this.commitRepository = commitRepository;
  }

  @Transactional
  public Commit save(Commit commit){
    commit.setId(0);//protection
    return commitRepository.save(commit);
  }

  public List<Commit> find(String appId, String clusterName, String namespaceName, Pageable page){
    return commitRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(appId, clusterName, namespaceName, page);
  }

  public List<Commit> find(String appId, String clusterName, String namespaceName,
      Date lastModifiedTime, Pageable page) {
    return commitRepository
        .findByAppIdAndClusterNameAndNamespaceNameAndDataChangeLastModifiedTimeGreaterThanEqualOrderByIdDesc(
            appId, clusterName, namespaceName, lastModifiedTime, page);
  }

  @Transactional
  public int batchDelete(String appId, String clusterName, String namespaceName, String operator){
    return commitRepository.batchDelete(appId, clusterName, namespaceName, operator);
  }

}
