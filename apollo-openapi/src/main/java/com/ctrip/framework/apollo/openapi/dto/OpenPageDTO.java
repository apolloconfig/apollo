/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.dto;

import java.util.Collections;
import java.util.List;

/**
 * @author mghio (mghio.dev@gmail.com)
 */
public class OpenPageDTO<T> {

    private final int page;
    private final int size;
    private final long total;
    private final List<T> content;

    public OpenPageDTO(int page, int size, long total, List<T> content) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public List<T> getContent() {
        return Collections.unmodifiableList(content);
    }

    public boolean hasContent() {
        return content != null && content.size() > 0;
    }

}
