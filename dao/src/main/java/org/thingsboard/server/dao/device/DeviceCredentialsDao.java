// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface DeviceCredentialsDao.
 */
public interface DeviceCredentialsDao extends Dao<DeviceCredentials> {

    /**
     * Save or update device credentials object
     *
     * @param tenantId the device tenant id
     * @param deviceCredentials the device credentials object
     * @return saved device credentials object
     */
    DeviceCredentials save(TenantId tenantId, DeviceCredentials deviceCredentials);

    DeviceCredentials saveAndFlush(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * Find device credentials by device id.
     *
     * @param deviceId the device id
     * @return the device credentials object
     */
    DeviceCredentials findByDeviceId(TenantId tenantId, UUID deviceId);

    /**
     * Find device credentials by credentials id.
     *
     * @param credentialsId the credentials id
     * @return the device credentials object
     */
    DeviceCredentials findByCredentialsId(TenantId tenantId, String credentialsId);

    DeviceCredentials removeByDeviceId(TenantId tenantId, DeviceId deviceId);

}
