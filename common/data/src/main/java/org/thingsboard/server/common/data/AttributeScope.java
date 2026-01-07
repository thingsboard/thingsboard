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
package org.thingsboard.server.common.data;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum AttributeScope {

    CLIENT_SCOPE(1),
    SERVER_SCOPE(2),
    SHARED_SCOPE(3);
    @Getter
    private final int id;

    private static final Map<Integer, AttributeScope> values = Arrays.stream(values())
            .collect(Collectors.toMap(AttributeScope::getId, scope -> scope));

    AttributeScope(int id) {
        this.id = id;
    }

    public static AttributeScope valueOf(int id) {
        return values.get(id);
    }

}
