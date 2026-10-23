// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteInstalledItemResult {

    /** Entities the delete kept, because other installed items use them too. */
    private List<KeptEntity> keptEntities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeptEntity {
        private EntityId entityId;
        // Names of the other installed items that use the entity
        private List<String> usedBy;
    }

}
