package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.anno.DataInfluenceTable;
import java.util.List;
import org.springframework.context.ApplicationListener;

public class AuditLogListener<T> implements ApplicationListener<AuditLogEvent<T>> {


  @Override
  public void onApplicationEvent(AuditLogEvent<T> event) {
    List<T> list = (List<T>) event.getSource();


  }

  public String getDataInfluenceTable(Class<T> clazz){
    return clazz.getAnnotation(DataInfluenceTable.class).tableName();
  }
}
