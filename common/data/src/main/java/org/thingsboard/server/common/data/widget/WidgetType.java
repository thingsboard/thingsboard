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
package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.WidgetTypeId;

import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
public class WidgetType extends BaseWidgetType {

    @Schema(description = "Complex JSON object that describes the widget type", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode descriptor;

    public WidgetType() {
        super();
    }

    public WidgetType(WidgetTypeId id) {
        super(id);
    }

    public WidgetType(BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    public WidgetType(WidgetType widgetType) {
        super(widgetType);
        this.descriptor = widgetType.getDescriptor();
    }

    @JsonIgnore
    public JsonNode getDefaultConfig() {
        return Optional.ofNullable(descriptor)
                .map(descriptor -> descriptor.get("defaultConfig"))
                .filter(JsonNode::isTextual).map(JsonNode::asText)
                .map(json -> {
                    try {
                        return mapper.readTree(json);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                }).orElse(null);
    }

    public void setDefaultConfig(JsonNode defaultConfig) {
        ((ObjectNode) descriptor).put("defaultConfig", defaultConfig.toString());
    }

}
