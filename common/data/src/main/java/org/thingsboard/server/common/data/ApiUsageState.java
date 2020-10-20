/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;

@ToString
@EqualsAndHashCode(callSuper = true)
public class ApiUsageState extends BaseData<ApiUsageStateId> implements HasTenantId {

    private static final long serialVersionUID = 8250339805336035966L;

    @Getter @Setter
    private TenantId tenantId;
    @Getter @Setter
    private EntityId entityId;
    @Getter @Setter
    private boolean transportEnabled;
    @Getter @Setter
    private boolean dbStorageEnabled;
    @Getter @Setter
    private boolean ruleEngineEnabled;
    @Getter @Setter
    private boolean jsExecEnabled;

    public ApiUsageState() {
        super();
    }

    public ApiUsageState(ApiUsageStateId id) {
        super(id);
    }

    public ApiUsageState(ApiUsageState ur) {
        super(ur);
        this.tenantId = ur.getTenantId();
        this.entityId = ur.getEntityId();
    }
}
