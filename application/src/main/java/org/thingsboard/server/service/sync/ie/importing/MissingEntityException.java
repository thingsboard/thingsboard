// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing;

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

public class MissingEntityException extends ImportServiceException {

    private static final long serialVersionUID = 3669135386955906022L;
    @Getter
    private final EntityId entityId;

    public MissingEntityException(EntityId entityId) {
        super("Referenced " + (entityId != null ? entityId.getEntityType() + " [" + entityId.getId() + "]" : "entity") + " not found");
        this.entityId = entityId;
    }
}
