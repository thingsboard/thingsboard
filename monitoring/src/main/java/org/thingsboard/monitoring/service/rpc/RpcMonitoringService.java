/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.monitoring.service.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringConfig;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringTarget;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.service.BaseMonitoringService;

@Service
@Slf4j
public class RpcMonitoringService extends BaseMonitoringService<RpcMonitoringConfig, RpcMonitoringTarget> {

    @Override
    protected BaseHealthChecker<?, ?> createHealthChecker(RpcMonitoringConfig config, RpcMonitoringTarget target) {
        return applicationContext.getBean(config.getTransportType().getServiceClass(), config, target);
    }

    @Override
    protected RpcMonitoringTarget createTarget(String baseUrl) {
        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setBaseUrl(baseUrl);
        return target;
    }

    @Override
    protected String getName() {
        return "rpc check";
    }

}
