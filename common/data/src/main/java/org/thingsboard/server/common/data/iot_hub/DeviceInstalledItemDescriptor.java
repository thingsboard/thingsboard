// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.iot_hub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class DeviceInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private List<EntityId> createdEntityIds;
    // Of createdEntityIds, the entities the user chose to use as they were: the package doesn't own them.
    // Read them with getUsedExistingEntityIdsIncludingLegacy(): installs without this field keep them only in installState.
    private List<EntityId> usedExistingEntityIds;
    private DashboardId dashboardId;
    private String selectedInstallMethod;
    private Map<String, JsonNode> installState;

    /**
     * The entities the user chose to use as they were. An install whose descriptor has no usedExistingEntityIds marks
     * them only in its install state, with the resolution "use-existing".
     */
    @JsonIgnore
    public Set<EntityId> getUsedExistingEntityIdsIncludingLegacy() {
        if (usedExistingEntityIds != null) {
            return new LinkedHashSet<>(usedExistingEntityIds);
        }
        Set<EntityId> usedExisting = new LinkedHashSet<>();
        if (createdEntityIds == null || installState == null) {
            return usedExisting;
        }
        Set<String> markedIds = new HashSet<>();
        installState.values().forEach(state -> {
            if (state != null && "use-existing".equals(state.path("resolution").asText())) {
                markedIds.add(state.path("entityOutput").path("id").asText());
            }
        });
        createdEntityIds.stream().filter(id -> markedIds.contains(id.getId().toString())).forEach(usedExisting::add);
        return usedExisting;
    }

}
