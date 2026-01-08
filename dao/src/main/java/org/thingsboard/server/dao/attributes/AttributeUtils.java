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
package org.thingsboard.server.dao.attributes;

import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.util.KvUtils;

import java.util.List;

public class AttributeUtils {

    @Deprecated(since = "3.7.0")
    public static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), uuid -> "Incorrect id " + uuid);
        Validator.validateString(scope, sc -> "Incorrect scope " + sc);
    }

    public static void validate(EntityId id, AttributeScope scope) {
        Validator.validateId(id.getId(), uuid -> "Incorrect id " + uuid);
        Validator.checkNotNull(scope, "Incorrect scope " + scope);
    }

    public static void validate(List<AttributeKvEntry> kvEntries,  boolean valueNoXssValidation) {
        kvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    public static void validate(AttributeKvEntry kvEntry, boolean valueNoXssValidation) {
        KvUtils.validate(kvEntry, valueNoXssValidation);
        if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }
}
