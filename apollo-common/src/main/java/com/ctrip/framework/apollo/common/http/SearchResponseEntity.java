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
package com.ctrip.framework.apollo.common.http;

import org.springframework.http.HttpStatus;

public class SearchResponseEntity<T> {

    private T body;
    private boolean hasMoreData;
    private Object message;
    private int code;

    public static <T> SearchResponseEntity<T> ok(T body){
        SearchResponseEntity<T> SearchResponseEntity = new SearchResponseEntity<>();
        SearchResponseEntity.message = HttpStatus.OK.getReasonPhrase();
        SearchResponseEntity.code = HttpStatus.OK.value();
        SearchResponseEntity.body = body;
        SearchResponseEntity.hasMoreData = false;
        return SearchResponseEntity;
    }

    public static <T> SearchResponseEntity<T> okWithMessage(T body, Object message){
        SearchResponseEntity<T> SearchResponseEntity = new SearchResponseEntity<>();
        SearchResponseEntity.message = message;
        SearchResponseEntity.code = HttpStatus.OK.value();
        SearchResponseEntity.body = body;
        SearchResponseEntity.hasMoreData = true;
        return SearchResponseEntity;
    }

    public static <T> SearchResponseEntity<T> error(HttpStatus httpCode, Object message){
        SearchResponseEntity<T> SearchResponseEntity = new SearchResponseEntity<>();
        SearchResponseEntity.message = message;
        SearchResponseEntity.code = httpCode.value();
        return SearchResponseEntity;
    }

    public int getCode() {
        return code;
    }

    public Object getMessage() {
        return message;
    }

    public T getBody() {
        return body;
    }

    public boolean isHasMoreData() {return hasMoreData;}

}