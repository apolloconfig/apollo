/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.common.dto;

import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class PageDTO<T> {
  private final long total;
  private final List<T> content;
  private final int page;
  private final int size;

  public PageDTO(List<T> content, Pageable pageable, long total) {
    this.total = total;
    this.content = content;
    this.page = pageable.getPageNumber();
    this.size = pageable.getPageSize();
  }

  public long getTotal() {
    return total;
  }

  public List<T> getContent() {
    return Collections.unmodifiableList(content);
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public boolean hasContent() {
    return content != null && content.size() > 0;
  }
}
