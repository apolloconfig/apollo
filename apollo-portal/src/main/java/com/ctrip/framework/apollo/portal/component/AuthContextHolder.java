package com.ctrip.framework.apollo.portal.component;

public final class AuthContextHolder {

    private static final ThreadLocal<String> AUTH_TYPE_HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
        // 不允许实例化
    }

    /** 写入当前线程的认证来源标识 */
    public static void setAuthType(String authType) {
        AUTH_TYPE_HOLDER.set(authType);
    }

    /** 读取当前线程的认证来源标识 */
    public static String getAuthType() {
        return AUTH_TYPE_HOLDER.get();
    }

    /** 清理当前线程变量，防止内存泄漏 */
    public static void clear() {
        AUTH_TYPE_HOLDER.remove();
    }
}
