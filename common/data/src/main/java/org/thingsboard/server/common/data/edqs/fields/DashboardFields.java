// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
@EqualsAndHashCode(callSuper = true)
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
