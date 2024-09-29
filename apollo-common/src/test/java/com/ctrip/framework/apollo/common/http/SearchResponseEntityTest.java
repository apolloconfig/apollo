/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.ctrip.framework.apollo.common.http;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SearchResponseEntityTest {

    @Test
    public void testOk_WithValidBody_ShouldReturnOkResponse() {
        String body = "test body";
        SearchResponseEntity<String> response = SearchResponseEntity.ok(body);

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals(HttpStatus.OK.getReasonPhrase(), response.getMessage());
        assertEquals(body, response.getBody());
        assertFalse(response.isHasMoreData());
    }

    @Test
    public void testOkWithMessage_WithValidBodyAndMessage_ShouldReturnOkResponseWithMessage() {
        String body = "test body";
        String message = "test message";
        SearchResponseEntity<String> response = SearchResponseEntity.okWithMessage(body, message);

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals(message, response.getMessage());
        assertEquals(body, response.getBody());
        assertTrue(response.isHasMoreData());
    }

    @Test
    public void testError_WithValidCodeAndMessage_ShouldReturnErrorResponse() {
        HttpStatus httpCode = HttpStatus.BAD_REQUEST;
        String message = "error message";
        SearchResponseEntity<Object> response = SearchResponseEntity.error(httpCode, message);

        assertEquals(httpCode.value(), response.getCode());
        assertEquals(message, response.getMessage());
        assertEquals(null, response.getBody());
        assertFalse(response.isHasMoreData());
    }
}
