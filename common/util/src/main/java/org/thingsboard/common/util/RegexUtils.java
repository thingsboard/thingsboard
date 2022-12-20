/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegexUtils {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");


    public static String replace(String s, Pattern pattern, UnaryOperator<String> replacer) {
        return pattern.matcher(s).replaceAll(matchResult -> {
            return replacer.apply(matchResult.group());
        });
    }

    public static boolean matches(String input, Pattern pattern) {
        return pattern.matcher(input).matches();
    }

}
