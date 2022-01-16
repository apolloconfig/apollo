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
package com.ctrip.framework.apollo.core.spi;

/**
 * {@code Ordered} is an interface that can be implemented by objects that
 * should be <em>orderable</em>, for example in a {@code Collection}.
 *
 * <p>The actual {@link #getOrder() order} can be interpreted as prioritization,
 * with the first object (with the lowest order value) having the highest
 * priority.
 *
 * @since 1.0.0
 */
public interface Ordered {
  /**
   * Useful constant for the highest precedence value.
   * @see java.lang.Integer#MIN_VALUE
   */
  int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

  /**
   * Useful constant for the lowest precedence value.
   * @see java.lang.Integer#MAX_VALUE
   */
  int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

  /**
   * Get the order value of this object.
   * <p>Higher values are interpreted as lower priority. As a consequence,
   * the object with the lowest value has the highest priority (somewhat
   * analogous to Servlet {@code load-on-startup} values).
   * <p>Same order values will result in arbitrary sort positions for the
   * affected objects.
   * @return the order value
   * @see #HIGHEST_PRECEDENCE
   * @see #LOWEST_PRECEDENCE
   */
  int getOrder();
}
