// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AssetProfileEvictEvent {

    private final TenantId tenantId;
    private final String newName;
    private final String oldName;
    private final AssetProfileId assetProfileId;
    private final boolean defaultProfile;
    private AssetProfile savedAssetProfile;

}
