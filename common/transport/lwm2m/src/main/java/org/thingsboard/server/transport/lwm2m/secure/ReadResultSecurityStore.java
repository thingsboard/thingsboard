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
package org.thingsboard.server.transport.lwm2m.secure;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.DEFAULT_MODE;

@Data
public class ReadResultSecurityStore {
    private ValidateDeviceCredentialsResponseMsg msg;
    private SecurityInfo securityInfo;
    @Builder.Default
    private int securityMode = DEFAULT_MODE.code;

    /** bootstrap */
    DeviceProfile deviceProfile;
    JsonObject bootstrapJsonCredential;
    String endPoint;
    BootstrapConfig bootstrapConfig;
}
