/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import java.util.UUID;

import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.model.DeviceCredentialsEntity;

/**
 * The Interface DeviceCredentialsDao.
 *
 * @param <T> the generic type
 */
public interface DeviceCredentialsDao extends Dao<DeviceCredentialsEntity> {

    /**
     * Save or update device credentials object
     *
     * @param deviceCredentials the device credentials object
     * @return saved device credentials object
     */
    DeviceCredentialsEntity save(DeviceCredentials deviceCredentials);

    /**
     * Find device credentials by device id.
     *
     * @param deviceId the device id
     * @return the device credentials object
     */
    DeviceCredentialsEntity findByDeviceId(UUID deviceId);

    /**
     * Find device credentials by credentials id.
     *
     * @param credentialsId the credentials id
     * @return the device credentials object
     */
    DeviceCredentialsEntity findByCredentialsId(String credentialsId);

}
