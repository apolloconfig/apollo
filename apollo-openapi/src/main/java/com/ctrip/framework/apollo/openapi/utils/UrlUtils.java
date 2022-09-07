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
package com.ctrip.framework.apollo.openapi.utils;

import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Abner
 * @since  8/31/22
 */
public final class UrlUtils {

    private static final String ILLEGAL_KEY_REGEX = "[/\\\\]+";
    private static final Pattern ILLEGAL_KEY_PATTERN = Pattern.compile(ILLEGAL_KEY_REGEX,
        Pattern.MULTILINE);

    private UrlUtils() {
    }

    public static boolean hasIllegalChar(String key) {
        if (Strings.isNullOrEmpty(key)) {
            return false;
        }
        Matcher matcher = ILLEGAL_KEY_PATTERN.matcher(key);
        return matcher.find();
    }
}
