// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

public class MissingEntityException extends ImportServiceException {

    private static final long serialVersionUID = 3669135386955906022L;
    @Getter
    private final EntityId entityId;

    public MissingEntityException(EntityId entityId) {
        this.entityId = entityId;
    }
}
