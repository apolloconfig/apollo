package com.ctrip.framework.apollo.audit;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class ApolloAuditRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry, "com.ctrip.framework.apollo.audit.entity",
        "com.ctrip.framework.apollo.audit.repository");
  }
}
