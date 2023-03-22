/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.common.exception;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/22
 */
public class ItemNotFoundException extends NotFoundException{

    public ItemNotFoundException(String appId, String clusterName, String namespaceName, long itemId) {
        super("item not found for appId:%s clusterName:%s namespaceName:%s itemId:%s", appId, clusterName, namespaceName, itemId);
    }

    public ItemNotFoundException(String appId, String clusterName, String namespaceName, String itemKey) {
        super("item not found for appId:%s clusterName:%s namespaceName:%s itemKey:%s", appId, clusterName, namespaceName, itemKey);
    }

    public ItemNotFoundException(long itemId) {
        super("item not found for itemId:%s",itemId);
    }

    public ItemNotFoundException(String itemKey) {
        super("item not found for itemKey:%s",itemKey);
    }
}
