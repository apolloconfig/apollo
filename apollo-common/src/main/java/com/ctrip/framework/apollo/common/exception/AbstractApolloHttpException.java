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
package com.ctrip.framework.apollo.common.exception;

import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;

public abstract class AbstractApolloHttpException extends RuntimeException{

  private static final long serialVersionUID = -1713129594004951820L;
  
  protected HttpStatus httpStatus;

  /**
   * When args not empty, use {@link com.google.common.base.Strings#lenientFormat(String, Object...)}
   * to replace %s in msgTpl with args to set the error message. Otherwise, use msgTpl
   * to set the error message. e.g.: 
   * <pre>{@code new NotFoundException("... %s ... %s ... %s", "str", 0, 0.1)}</pre>
   * If the number of '%s' in `msgTpl` does not match args length, the '%s' string will be printed.
   */
  public AbstractApolloHttpException(String msgTpl, Object... args){
    super(args == null || args.length == 0 ? msgTpl : Strings.lenientFormat(msgTpl, args));
  }
  
  public AbstractApolloHttpException(String msg, Exception e){
    super(msg,e);
  }

  protected void setHttpStatus(HttpStatus httpStatus){
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
