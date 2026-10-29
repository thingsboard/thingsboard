// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

/** An entity of an installed device package as an update finds it, before it changes anything. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevicePackageEntityState {

    private String stepName;
    private EntityId entityId;
    private boolean exists;
    // Sent back with the update, which fails if someone changed the entity in between
    private Long version;
    // Changed since the package installed or last updated it; null when the install state has no checksum of it
    // (installed before checksums were kept)
    private Boolean modified;
    // Other installed device packages use it, so overwriting it affects them
    private boolean shared;

}
