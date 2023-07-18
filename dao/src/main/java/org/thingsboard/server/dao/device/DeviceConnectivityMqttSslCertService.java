/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;

@Service
@Slf4j
public class DeviceConnectivityMqttSslCertService implements TbDeviceConnectivitySslCertService {

    private String certificate;
    @Autowired
    private DeviceConnectivityConfiguration deviceConnectivityConfiguration;

    @PostConstruct
    private void postConstruct() throws IOException {
        String sslCertPath = deviceConnectivityConfiguration.getConnectivity()
                .get(MQTTS)
                .getSslCertPath();
        if (sslCertPath != null && ResourceUtils.resourceExists(this, sslCertPath)) {
            certificate = FileUtils.readFileToString(new File(sslCertPath), StandardCharsets.UTF_8);
        }
    }

    @Override
    public String getMqttSslCertificate() {
        return certificate;
    }
}
