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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.function.Function;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class BaseDataWithAdditionalInfo<I extends UUIDBased> extends BaseData<I> implements HasAdditionalInfo {

    @NoXss
    private JsonNode additionalInfo;

    public BaseDataWithAdditionalInfo() {
        super();
    }

    public BaseDataWithAdditionalInfo(I id) {
        super(id);
    }

    public BaseDataWithAdditionalInfo(BaseDataWithAdditionalInfo<I> baseData) {
        super(baseData);
        this.additionalInfo = baseData.getAdditionalInfo();
    }

    public void setAdditionalInfoField(String field, JsonNode value) {
        JsonNode additionalInfo = getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = mapper.createObjectNode();
        }
        ((ObjectNode) additionalInfo).set(field, value);
        setAdditionalInfo(additionalInfo);
    }

    public <T> T getAdditionalInfoField(String field, Function<JsonNode, T> mapper, T defaultValue) {
        JsonNode additionalInfo = getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(field)) {
            return mapper.apply(additionalInfo.get(field));
        }
        return defaultValue;
    }

}
