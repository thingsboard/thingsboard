// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@NoArgsConstructor
public class VersionedEntityInfo {
    private EntityId externalId;

    public VersionedEntityInfo(EntityId externalId) {
        this.externalId = externalId;
    }
}
