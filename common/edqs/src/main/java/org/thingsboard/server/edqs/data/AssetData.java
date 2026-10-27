// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.AssetFields;

import java.util.UUID;

@ToString(callSuper = true)
public class AssetData extends ProfileAwareData<AssetFields> {

    public AssetData(UUID id) {
        super(id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
