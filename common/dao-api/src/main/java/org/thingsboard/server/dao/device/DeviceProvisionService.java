// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;

public interface DeviceProvisionService {

    ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) throws ProvisionFailedException;

    ProvisionResponse provisionDeviceViaX509Chain(DeviceProfile deviceProfile, ProvisionRequest provisionRequest) throws ProvisionFailedException;
}
