/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.state;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;

import java.util.Optional;

/**
 * Created by ashvayka on 01.05.18.
 */
public interface DeviceStateService {

    void onDeviceAdded(Device device);

    void onDeviceUpdated(Device device);

    void onDeviceDeleted(Device device);

    void onDeviceConnect(DeviceId deviceId);

    void onDeviceActivity(DeviceId deviceId);

    void onDeviceDisconnect(DeviceId deviceId);

    void onDeviceInactivityTimeoutUpdate(DeviceId deviceId, long inactivityTimeout);

    void onClusterUpdate();

    void onRemoteMsg(ServerAddress serverAddress, byte[] bytes);
}
