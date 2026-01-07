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
package org.thingsboard.server.common.data.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Rpc extends BaseData<RpcId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Device Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private DeviceId deviceId;
    @Schema(description = "Expiration time of the request.", accessMode = Schema.AccessMode.READ_ONLY)
    private long expirationTime;
    @Schema(description = "The request body that will be used to send message to device.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode request;
    @Schema(description = "The response from the device.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode response;
    @Schema(description = "The current status of the RPC call.", accessMode = Schema.AccessMode.READ_ONLY)
    private RpcStatus status;
    @Schema(description = "Additional info used in the rule engine to process the updates to the RPC state.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode additionalInfo;

    public Rpc() {
        super();
    }

    public Rpc(RpcId id) {
        super(id);
    }

    public Rpc(Rpc rpc) {
        super(rpc);
        this.tenantId = rpc.getTenantId();
        this.deviceId = rpc.getDeviceId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    @Schema(description = "JSON object with the rpc Id. Referencing non-existing rpc Id will cause error.")
    @Override
    public RpcId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the rpc creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
