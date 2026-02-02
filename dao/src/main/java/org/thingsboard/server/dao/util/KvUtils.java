/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.NoXssValidator;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class KvUtils {

    private static final Cache<String, Boolean> validatedKeys;

    static {
        validatedKeys = Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(50000).build();
    }

    public static void validate(List<? extends KvEntry> tsKvEntries, boolean valueNoXssValidation) {
        tsKvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    public static void validate(KvEntry tsKvEntry, boolean valueNoXssValidation) {
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }

        String key = tsKvEntry.getKey();

        if (StringUtils.isBlank(key)) {
            throw new DataValidationException("Key can't be null or empty");
        }

        if (key.length() > 255) {
            throw new DataValidationException("Validation error: key length must be equal or less than 255");
        }

        Boolean isValid = validatedKeys.asMap().get(key);
        if (isValid == null) {
            isValid = NoXssValidator.isValid(key);
            validatedKeys.put(key, isValid);
        }
        if (!isValid) {
            throw new DataValidationException("Validation error: key is malformed");
        }

        if (valueNoXssValidation) {
            Object value = tsKvEntry.getValue();
            if (value instanceof CharSequence || value instanceof JsonNode) {
                if (!NoXssValidator.isValid(value.toString())) {
                    throw new DataValidationException("Validation error: value is malformed");
                }
            }
        }
    }
}
