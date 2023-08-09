package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.entity.BaseEntity;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ApolloAuditLogService {

  private final ApolloAuditLogRepository logRepository;
  private final ApolloAuditLogDataInfluenceRepository dataInfluenceRepository;

  public ApolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    this.logRepository = logRepository;
    this.dataInfluenceRepository = dataInfluenceRepository;
  }

  public void logSpan(ApolloAuditSpan span) {

    ApolloAuditLog auditLog = new ApolloAuditLog();
    auditLog.setTraceId(span.context().getTraceId());
    auditLog.setSpanId(span.context().getSpanId());
    auditLog.setParentSpanId(span.getParentId());
    auditLog.setFollowsFromSpanId(span.getFollowsFromId());
    auditLog.setOperator(span.context().getOperator());
    auditLog.setOpName(span.getOpName());
    auditLog.setOpType(span.getOpType().toString());
    auditLog.setDescription(span.getDescription());
    auditLog.setDataChangeCreatedTime(new Date());
    auditLog.setDataChangeCreatedBy(span.context().getOperator());

    logRepository.save(auditLog);
  }

  public List<ApolloAuditLog> findAuditLogByTraceId(String traceId) {
    return logRepository.findByTraceId(traceId);
  }

  public void logCreateDataInfluence(String spanId, String operator, List<Object> dataSet) {
    Date now = new Date();
    List<ApolloAuditLogDataInfluence> dataInfluenceList = new ArrayList<>();
    dataSet.forEach(
        data -> {
          Class<?> dataClass = data.getClass();
          long entityId = getIdByReflect(data);
          String tableName = dataClass.getAnnotation(ApolloAuditLogDataInfluenceTable.class).tableName();
          Arrays.stream(dataClass.getDeclaredFields()).filter(
              // only the field annotated 'ApolloAuditLogDataInfluenceTableField'
              field -> field.isAnnotationPresent(ApolloAuditLogDataInfluenceTableField.class)
          ).forEach(
              field -> {
                field.setAccessible(true);
                try {
                  String fieldName = field.getAnnotation(ApolloAuditLogDataInfluenceTableField.class).fieldName();
                  String fieldNewValue = field.get(data).toString();
                  // add a new dataInfluence
                  ApolloAuditLogDataInfluence dataInfluence = new ApolloAuditLogDataInfluence(
                      spanId, operator, now, tableName, entityId, fieldName,
                      null, fieldNewValue);
                  dataInfluenceList.add(dataInfluence);
                } catch (IllegalAccessException e) {
                  throw new RuntimeException(e);
                }
              }
          );
        }
    );
    dataInfluenceRepository.saveAll(dataInfluenceList);
  }

  public void logDeleteDataInfluence(String spanId, String operator, List<Object> dataSet) {
    Date now = new Date();
    List<ApolloAuditLogDataInfluence> dataInfluenceList = new ArrayList<>();
    dataSet.forEach(
        data -> {
          Class<?> dataClass = data.getClass();
          long entityId = getIdByReflect(data);
          String tableName = dataClass.getAnnotation(ApolloAuditLogDataInfluenceTable.class).tableName();
          Arrays.stream(dataClass.getDeclaredFields()).filter(
              // only the field annotated 'ApolloAuditLogDataInfluenceTableField'
              field -> field.isAnnotationPresent(ApolloAuditLogDataInfluenceTableField.class)
          ).forEach(
              field -> {
                try {
                  String fieldName = field.getAnnotation(ApolloAuditLogDataInfluenceTableField.class).fieldName();
                  String fieldOldValue = field.get(data).toString();
                  // add a new dataInfluence
                  ApolloAuditLogDataInfluence dataInfluence = new ApolloAuditLogDataInfluence(
                      spanId, operator, now, tableName, entityId, fieldName,
                      fieldOldValue, null);
                  dataInfluenceList.add(dataInfluence);
                } catch (IllegalAccessException e) {
                  throw new RuntimeException(e);
                }
              }
          );
        }
    );
    dataInfluenceRepository.saveAll(dataInfluenceList);
  }

  public void logUpdateDataInfluence(String spanId, String operator, List<Object> olds, List<Object> news) {
    Date now = new Date();
    List<ApolloAuditLogDataInfluence> dataInfluenceList = new ArrayList<>();
    for (Object oldObj : olds) {
      for (Object newObj : news) {
        if(getIdByReflect(newObj) == getIdByReflect(newObj) && newObj.getClass().equals(oldObj.getClass())) {
          // same entity
          Class<?> dataClass = newObj.getClass();
          long entityId = getIdByReflect(newObj);
          String tableName = dataClass.getAnnotation(ApolloAuditLogDataInfluenceTable.class).tableName();
          Arrays.stream(dataClass.getDeclaredFields()).filter(
              // only the field annotated 'ApolloAuditLogDataInfluenceTableField'
              field -> field.isAnnotationPresent(ApolloAuditLogDataInfluenceTableField.class)
          ).forEach(
              field -> {
                try {
                  String fieldName = field.getAnnotation(ApolloAuditLogDataInfluenceTableField.class).fieldName();
                  String fieldOldValue = field.get(oldObj).toString();
                  String fieldNewValue = field.get(newObj).toString();
                  // add a new dataInfluence
                  ApolloAuditLogDataInfluence dataInfluence = new ApolloAuditLogDataInfluence(
                      spanId, operator, now, tableName, entityId, fieldName,
                      fieldOldValue, fieldNewValue);
                  dataInfluenceList.add(dataInfluence);
                } catch (IllegalAccessException e) {
                  throw new RuntimeException(e);
                }
              }
          );
        }
      }
    }
    dataInfluenceRepository.saveAll(dataInfluenceList);
  }

  static long getIdByReflect(Object o) {
    Class<?> clazz = o.getClass();
    try {
      Field idField = null;
      if (Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
        idField = o.getClass().getDeclaredField("id");
        idField.setAccessible(true); // Enable access to private fields
        return (long) idField.get(o);
      } else if (Arrays.stream(clazz.getSuperclass().getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
        idField = o.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true); // Enable access to private fields
        return (long) idField.get(o);
      }
      return -1;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
      return -1; // Return a default value or handle the error
    }
  }

}
