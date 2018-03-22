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
package org.thingsboard.server.service.security.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.transport.auth.DeviceAuthResult;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.Optional;

@Service
@Slf4j
public class DefaultDeviceAuthService implements DeviceAuthService {

    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Override
    public DeviceAuthResult process(DeviceCredentialsFilter credentialsFilter) {
        log.trace("Lookup device credentials using filter {}", credentialsFilter);
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsFilter.getCredentialsId());
        if (credentials != null) {
            log.trace("Credentials found {}", credentials);
            if (credentials.getCredentialsType() == credentialsFilter.getCredentialsType()) {
                switch (credentials.getCredentialsType()) {
                    case ACCESS_TOKEN:
                        // Credentials ID matches Credentials value in this
                        // primitive case;
                        return DeviceAuthResult.of(credentials.getDeviceId());
                    case X509_CERTIFICATE:
                        return DeviceAuthResult.of(credentials.getDeviceId());
                    default:
                        return DeviceAuthResult.of("Credentials Type is not supported yet!");
                }
            } else {
                return DeviceAuthResult.of("Credentials Type mismatch!");
            }
        } else {
            log.trace("Credentials not found!");
            return DeviceAuthResult.of("Credentials Not Found!");
        }
    }

    @Override
    public Optional<Device> findDeviceById(DeviceId deviceId) {
        return Optional.ofNullable(deviceService.findDeviceById(deviceId));
    }
}
