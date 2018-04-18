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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.DeviceStatusQuery;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.device.DeviceStatusQuery.Status.OFFLINE;
import static org.thingsboard.server.common.data.device.DeviceStatusQuery.Status.ONLINE;

@Service
public class DeviceOfflineServiceImpl implements DeviceOfflineService {

    @Autowired
    private DeviceDao deviceDao;

    @Override
    public void online(Device device, boolean isUpdate) {
        long current = System.currentTimeMillis();
        device.setLastConnectTs(current);
        if(isUpdate) {
            device.setLastUpdateTs(current);
        }
        deviceDao.saveDeviceStatus(device);
    }

    @Override
    public void offline(Device device) {
        online(device, false);
    }

    @Override
    public ListenableFuture<List<Device>> findOfflineDevices(UUID tenantId, DeviceStatusQuery.ContactType contactType, long offlineThreshold) {
        DeviceStatusQuery statusQuery = new DeviceStatusQuery(OFFLINE, contactType, offlineThreshold);
        return deviceDao.findDevicesByTenantIdAndStatus(tenantId, statusQuery);
    }

    @Override
    public ListenableFuture<List<Device>> findOnlineDevices(UUID tenantId, DeviceStatusQuery.ContactType contactType, long offlineThreshold) {
        DeviceStatusQuery statusQuery = new DeviceStatusQuery(ONLINE, contactType, offlineThreshold);
        return deviceDao.findDevicesByTenantIdAndStatus(tenantId, statusQuery);
    }
}
