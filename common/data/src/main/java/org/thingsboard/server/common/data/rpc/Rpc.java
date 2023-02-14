/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
public class Rpc extends BaseData<RpcId> implements HasTenantId {

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @ApiModelProperty(position = 4, value = "JSON object with Device Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private DeviceId deviceId;
    @ApiModelProperty(position = 5, value = "Expiration time of the request.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private long expirationTime;
    @ApiModelProperty(position = 6, value = "The request body that will be used to send message to device.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode request;
    @ApiModelProperty(position = 7, value = "The response from the device.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode response;
    @ApiModelProperty(position = 8, value = "The current status of the RPC call.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RpcStatus status;
    @ApiModelProperty(position = 9, value = "Additional info used in the rule engine to process the updates to the RPC state.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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

    @ApiModelProperty(position = 1, value = "JSON object with the rpc Id. Referencing non-existing rpc Id will cause error.")
    @Override
    public RpcId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the rpc creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
