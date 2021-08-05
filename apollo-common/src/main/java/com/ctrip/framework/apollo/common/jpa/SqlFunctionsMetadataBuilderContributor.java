package com.ctrip.framework.apollo.common.jpa;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author nisiyong
 */
public class SqlFunctionsMetadataBuilderContributor implements MetadataBuilderContributor {

  @Override
  public void contribute(MetadataBuilder metadataBuilder) {
    metadataBuilder.applySqlFunction("NOW",
        new StandardSQLFunction("NOW", StandardBasicTypes.INTEGER));
  }
}
