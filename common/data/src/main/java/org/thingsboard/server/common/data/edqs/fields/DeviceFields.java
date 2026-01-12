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
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class DeviceFields extends AbstractEntityFields implements ProfileAwareFields {

    private String label;
    private String type;
    private UUID deviceProfileId;
    private String additionalInfo;

    @JsonIgnore
    @Override
    public String getProfileName() {
        return type;
    }

    @JsonIgnore
    @Override
    public UUID getProfileId() {
        return deviceProfileId;
    }

    public DeviceFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version, String type,
                        String label, UUID deviceProfileId, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId,  name, version);
        this.label = label;
        this.type = type;
        this.deviceProfileId = deviceProfileId;
        this.additionalInfo = getText(additionalInfo);
    }
}
