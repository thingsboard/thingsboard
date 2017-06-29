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


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.EncryptionUtil;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CREDENTIALS_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class DeviceCredentialsServiceImpl implements DeviceCredentialsService {

    @Autowired
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired
    private DeviceService deviceService;

    @Override
    public DeviceCredentials findDeviceCredentialsByDeviceId(DeviceId deviceId) {
        log.trace("Executing findDeviceCredentialsByDeviceId [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        return deviceCredentialsDao.findByDeviceId(deviceId.getId());
    }

    @Override
    @Cacheable(cacheNames = DEVICE_CREDENTIALS_CACHE, unless="#result == null")
    public DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId) {
        log.trace("Executing findDeviceCredentialsByCredentialsId [{}]", credentialsId);
        validateString(credentialsId, "Incorrect credentialsId " + credentialsId);
        return deviceCredentialsDao.findByCredentialsId(credentialsId);
    }

    @Override
    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, keyGenerator="previousDeviceCredentialsId", beforeInvocation = true)
    public DeviceCredentials updateDeviceCredentials(DeviceCredentials deviceCredentials) {
        return saveOrUpdare(deviceCredentials);
    }

    @Override
    public DeviceCredentials createDeviceCredentials(DeviceCredentials deviceCredentials) {
        return saveOrUpdare(deviceCredentials);
    }

    private DeviceCredentials saveOrUpdare(DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            formatCertData(deviceCredentials);
        }
        log.trace("Executing updateDeviceCredentials [{}]", deviceCredentials);
        credentialsValidator.validate(deviceCredentials);
        return deviceCredentialsDao.save(deviceCredentials);
    }

    private void formatCertData(DeviceCredentials deviceCredentials) {
        String cert = EncryptionUtil.trimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        deviceCredentials.setCredentialsId(sha3Hash);
        deviceCredentials.setCredentialsValue(cert);
    }

    @Override
    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, key="#deviceCredentials.credentialsId")
    public void deleteDeviceCredentials(DeviceCredentials deviceCredentials) {
        log.trace("Executing deleteDeviceCredentials [{}]", deviceCredentials);
        deviceCredentialsDao.removeById(deviceCredentials.getUuidId());
    }

    private DataValidator<DeviceCredentials> credentialsValidator =
            new DataValidator<DeviceCredentials>() {

                @Override
                protected void validateCreate(DeviceCredentials deviceCredentials) {
                    DeviceCredentials existingCredentialsEntity = deviceCredentialsDao.findByCredentialsId(deviceCredentials.getCredentialsId());
                    if (existingCredentialsEntity != null) {
                        throw new DataValidationException("Create of existent device credentials!");
                    }
                }

                @Override
                protected void validateUpdate(DeviceCredentials deviceCredentials) {
                    DeviceCredentials existingCredentials = deviceCredentialsDao.findById(deviceCredentials.getUuidId());
                    if (existingCredentials == null) {
                        throw new DataValidationException("Unable to update non-existent device credentials!");
                    }
                    DeviceCredentials sameCredentialsId = deviceCredentialsDao.findByCredentialsId(deviceCredentials.getCredentialsId());
                    if (sameCredentialsId != null && !sameCredentialsId.getUuidId().equals(deviceCredentials.getUuidId())) {
                        throw new DataValidationException("Specified credentials are already registered!");
                    }
                }

                @Override
                protected void validateDataImpl(DeviceCredentials deviceCredentials) {
                    if (deviceCredentials.getDeviceId() == null) {
                        throw new DataValidationException("Device credentials should be assigned to device!");
                    }
                    if (deviceCredentials.getCredentialsType() == null) {
                        throw new DataValidationException("Device credentials type should be specified!");
                    }
                    if (StringUtils.isEmpty(deviceCredentials.getCredentialsId())) {
                        throw new DataValidationException("Device credentials id should be specified!");
                    }
                    Device device = deviceService.findDeviceById(deviceCredentials.getDeviceId());
                    if (device == null) {
                        throw new DataValidationException("Can't assign device credentials to non-existent device!");
                    }
                }
            };

}
