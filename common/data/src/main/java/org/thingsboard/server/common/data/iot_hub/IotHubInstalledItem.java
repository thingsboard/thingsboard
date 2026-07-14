/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
