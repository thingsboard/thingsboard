/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequest implements Serializable {

    private static final long serialVersionUID = -7089247105087346214L;

    private final UUID id;
    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final boolean oneway;
    private final long expirationTime;
    private final ToDeviceRpcRequestBody body;
    private final boolean persisted;
    private final Integer retries;
    @JsonIgnore
    private final String additionalInfo;
}
