// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ComplexVersionCreateRequest extends VersionCreateRequest {

    // Default sync strategy
    private SyncStrategy syncStrategy;
    private Map<EntityType, EntityTypeVersionCreateConfig> entityTypes;

    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.COMPLEX;
    }

}
