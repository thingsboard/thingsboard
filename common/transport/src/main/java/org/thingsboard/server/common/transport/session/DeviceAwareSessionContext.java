/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.session;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthResult;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    private volatile TransportProtos.DeviceInfoProto deviceInfo;

    public long getDeviceIdMSB() {
        return deviceInfo.getDeviceIdMSB();
    }

    public long getDeviceIdLSB() {
        return deviceInfo.getDeviceIdLSB();
    }

    public boolean isConnected() {
        return deviceInfo != null;
    }
}
