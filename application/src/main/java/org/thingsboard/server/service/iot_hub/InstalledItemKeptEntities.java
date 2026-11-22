// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

/**
 * What deleting an installed item keeps: what other installed items use too, what the user had before it, and what other
 * tenant entities refer to (a LoRaWAN integration creates its devices on the first uplink, not the package).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstalledItemKeptEntities {

    private List<KeptEntity> entities;
    // How many other installed items use at least one of the kept entities
    private int usedByItemsCount;
    // How many of the kept entities the user had before and the package took as they were (use existing)
    private int usedExistingCount;
    // What other tenant entities refer to: the package's device profiles that devices use, and the default rule chains and
    // dashboard of a kept profile. Apart from entities, which an update reads as shared with other packages
    private List<KeptEntity> referencedEntities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeptEntity {
        private EntityId entityId;
        private String name;
    }

}
