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
package org.thingsboard.server.common.transport.quota.host;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.quota.AbstractQuotaService;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
@Service
@Slf4j
public class HostRequestsQuotaService extends AbstractQuotaService {

    public HostRequestsQuotaService(HostRequestIntervalRegistry requestRegistry, HostRequestLimitPolicy requestsPolicy,
                                    HostIntervalRegistryCleaner registryCleaner, HostIntervalRegistryLogger registryLogger,
                                    @Value("${quota.host.enabled}") boolean enabled) {
        super(requestRegistry, requestsPolicy, registryCleaner, registryLogger, enabled);
    }

}
