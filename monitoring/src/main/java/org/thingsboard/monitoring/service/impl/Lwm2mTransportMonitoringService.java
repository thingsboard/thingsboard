/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.monitoring.service.impl;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.Lwm2mTransportMonitoringServiceConfig;
import org.thingsboard.monitoring.service.TransportMonitoringService;
import org.thingsboard.monitoring.client.Lwm2mClient;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Lwm2mTransportMonitoringService extends TransportMonitoringService<Lwm2mTransportMonitoringServiceConfig> {

    private Lwm2mClient lwm2mClient;

    protected Lwm2mTransportMonitoringService(Lwm2mTransportMonitoringServiceConfig config, MonitoringTargetConfig target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        lwm2mClient = new Lwm2mClient(target.getBaseUrl(), target.getDevice().getAccessToken());
        lwm2mClient.initClient();
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        lwm2mClient.send(payload);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return testValue;
    }

    @Override
    protected void destroyClient() throws Exception {
        lwm2mClient.destroy();
        lwm2mClient = null;
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
