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
package org.thingsboard.server.common.transport.quota.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.KeyBasedIntervalRegistry;

@Component
public class TenantMsgsIntervalRegistry extends KeyBasedIntervalRegistry {

    public TenantMsgsIntervalRegistry(@Value("${quota.rule.tenant.intervalMs}") long intervalDurationMs,
                                      @Value("${quota.rule.tenant.ttlMs}") long ttlMs,
                                      @Value("${quota.rule.tenant.whitelist}") String whiteList,
                                      @Value("${quota.rule.tenant.blacklist}") String blackList) {
        super(intervalDurationMs, ttlMs, whiteList, blackList, "Rule Tenant");
    }
}
