/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class FirmwareInfo extends SearchTextBasedWithAdditionalInfo<FirmwareId> implements HasTenantId {

    private static final long serialVersionUID = 3168391583570815419L;

    private TenantId tenantId;
    private String title;
    private String version;
    private boolean hasData;

    public FirmwareInfo() {
        super();
    }

    public FirmwareInfo(FirmwareId id) {
        super(id);
    }

    public FirmwareInfo(FirmwareInfo firmwareInfo) {
        super(firmwareInfo);
        this.tenantId = firmwareInfo.getTenantId();
        this.title = firmwareInfo.getTitle();
        this.version = firmwareInfo.getVersion();
    }

    @Override
    public String getSearchText() {
        return title;
    }
}
