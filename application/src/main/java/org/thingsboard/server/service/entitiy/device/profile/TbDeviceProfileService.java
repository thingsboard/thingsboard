// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.device.profile;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbDeviceProfileService {

    DeviceProfile save(DeviceProfile deviceProfile, SecurityUser user) throws Exception;

    void delete(DeviceProfile deviceProfile, User user) throws ThingsboardException;

    DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException;
}
