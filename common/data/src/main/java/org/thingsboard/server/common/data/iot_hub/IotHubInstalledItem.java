// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class IotHubInstalledItem extends BaseData<IotHubInstalledItemId> implements HasTenantId {

    private TenantId tenantId;
    private UUID itemId;
    private UUID itemVersionId;
    private String itemName;
    private String itemType;
    private String version;
    private IotHubInstalledItemDescriptor descriptor;

    public IotHubInstalledItem() {
        super();
    }

    public IotHubInstalledItem(IotHubInstalledItemId id) {
        super(id);
    }

}
