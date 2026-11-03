// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionLoadRequest extends VersionLoadRequest {

    private EntityId externalEntityId;

    private VersionLoadConfig config;

    @Override
    public VersionLoadRequestType getType() {
        return VersionLoadRequestType.SINGLE_ENTITY;
    }

}
