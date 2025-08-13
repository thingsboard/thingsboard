/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class DynamicValue<T> implements Serializable {

    private T resolvedValue;

    private final DynamicValueSourceType sourceType;
    @NoXss
    private final String sourceAttribute;
    private final boolean inherit;

    public DynamicValue(DynamicValueSourceType sourceType, String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
        this.sourceType = sourceType;
        this.inherit = false;
    }

}
