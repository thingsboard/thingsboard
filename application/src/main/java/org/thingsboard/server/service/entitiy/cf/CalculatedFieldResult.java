/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.cf;

import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;

@Data
public class CalculatedFieldResult {

    private String name;
    private String type;
    private AttributeScope scope;
    private String value;

    public static CalculatedFieldResult createAttributesResult(String name, AttributeScope scope, String value) {
        CalculatedFieldResult result = new CalculatedFieldResult();
        result.name = name;
        result.type = "ATTRIBUTES";
        result.scope = scope;
        result.value = value;
        return result;
    }

    public static CalculatedFieldResult createTimeSeriesResult(String name, String value) {
        CalculatedFieldResult result = new CalculatedFieldResult();
        result.name = name;
        result.type = "TIME_SERIES";
        result.value = value;
        return result;
    }

}
