// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

/** What deleting an installed item keeps, because other installed items use it too. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstalledItemKeptEntities {

    private List<KeptEntity> entities;
    // How many other installed items use at least one of the kept entities
    private int usedByItemsCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeptEntity {
        private EntityId entityId;
        private String name;
    }

}
