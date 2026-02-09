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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
@NoArgsConstructor
@SuperBuilder
public class DashboardFields extends AbstractEntityFields {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private List<UUID> assignedCustomerIds;

    public DashboardFields(UUID id, long createdTime, UUID tenantId, String assignedCustomers, String name, Long version) {
        super(id, createdTime, tenantId, name, version);
        this.assignedCustomerIds = getCustomerIds(assignedCustomers);
    }

    private static List<UUID> getCustomerIds(String assignedCustomers) {
        List<UUID> ids = new ArrayList<>();
        if (assignedCustomers == null || assignedCustomers.isEmpty()) {
            return ids;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(assignedCustomers);
            for (JsonNode node : rootNode) {
                String idStr = node.path("customerId").path("id").asText();
                if (!idStr.isEmpty()) {
                    ids.add(UUID.fromString(idStr));
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }
}
