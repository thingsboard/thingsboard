// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

@SuppressWarnings("rawtypes")
public class LoadEntityException extends RuntimeException {

    private static final long serialVersionUID = -1749719992370409504L;
    @Getter
    private final EntityId externalId;

    public LoadEntityException(EntityId externalId, Throwable cause) {
        super(cause);
        this.externalId = externalId;
    }
}
