/*
 * Copyright 2025 Apollo Authors
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
