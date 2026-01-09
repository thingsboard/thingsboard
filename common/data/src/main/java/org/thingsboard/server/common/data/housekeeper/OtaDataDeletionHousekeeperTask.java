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
package org.thingsboard.server.common.data.housekeeper;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtaDataDeletionHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = 5392737356095687729L;

    private Long oid;

    public OtaDataDeletionHousekeeperTask(TenantId tenantId, OtaPackageId otaPackageId, Long oid) {
        super(tenantId, otaPackageId, HousekeeperTaskType.DELETE_OTA_DATA);
        this.oid = oid;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " (OID: " + oid + ")";
    }

}
