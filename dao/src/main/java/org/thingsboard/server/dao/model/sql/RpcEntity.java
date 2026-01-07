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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.RPC_ADDITIONAL_INFO;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_DEVICE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_EXPIRATION_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_REQUEST;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_RESPONSE;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_STATUS;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RPC_TABLE_NAME)
public class RpcEntity extends BaseSqlEntity<Rpc> implements BaseEntity<Rpc> {

    @Column(name = RPC_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = RPC_DEVICE_ID)
    private UUID deviceId;

    @Column(name = RPC_EXPIRATION_TIME)
    private long expirationTime;

    @Convert(converter = JsonConverter.class)
    @Column(name = RPC_REQUEST)
    private JsonNode request;

    @Convert(converter = JsonConverter.class)
    @Column(name = RPC_RESPONSE)
    private JsonNode response;

    @Enumerated(EnumType.STRING)
    @Column(name = RPC_STATUS)
    private RpcStatus status;

    @Convert(converter = JsonConverter.class)
    @Column(name = RPC_ADDITIONAL_INFO)
    private JsonNode additionalInfo;

    public RpcEntity() {
        super();
    }

    public RpcEntity(Rpc rpc) {
        this.setUuid(rpc.getUuidId());
        this.createdTime = rpc.getCreatedTime();
        this.tenantId = rpc.getTenantId().getId();
        this.deviceId = rpc.getDeviceId().getId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    @Override
    public Rpc toData() {
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(createdTime);
        rpc.setTenantId(TenantId.fromUUID(tenantId));
        rpc.setDeviceId(new DeviceId(deviceId));
        rpc.setExpirationTime(expirationTime);
        rpc.setRequest(request);
        rpc.setResponse(response);
        rpc.setStatus(status);
        rpc.setAdditionalInfo(additionalInfo);
        return rpc;
    }
}
