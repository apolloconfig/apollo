package com.ctrip.framework.apollo.portal.service;

/**
 *
 * Used in ConfigsImportService class to remove duplication from importEntities method
 */
@FunctionalInterface
public interface ImportTaskService<T> {
    void execute(T entity);
}
