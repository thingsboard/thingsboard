// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionCreateRequest extends VersionCreateRequest {

    private EntityId entityId;
    private VersionCreateConfig config;

    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.SINGLE_ENTITY;
    }

}
