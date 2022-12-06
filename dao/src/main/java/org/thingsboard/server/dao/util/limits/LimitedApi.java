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
package org.thingsboard.server.dao.util.limits;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.thingsboard.server.dao.util.limits.LimitLevel.CUSTOMER;
import static org.thingsboard.server.dao.util.limits.LimitLevel.GENERAL;
import static org.thingsboard.server.dao.util.limits.LimitLevel.TENANT;

@RequiredArgsConstructor
public enum LimitedApi {
    REST_REQUESTS(Map.of(
            TENANT, DefaultTenantProfileConfiguration::getTenantServerRestLimitsConfiguration,
            CUSTOMER, DefaultTenantProfileConfiguration::getCustomerServerRestLimitsConfiguration
    )),
    ENTITY_EXPORT(Map.of(
            TENANT, DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit
    )),
    ENTITY_IMPORT(Map.of(
            TENANT, DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit
    )),
    WS_UPDATES_PER_SESSION(Map.of(
            GENERAL, DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit
    )),
    CASSANDRA_QUERIES(Map.of(
            TENANT, DefaultTenantProfileConfiguration::getCassandraQueryTenantRateLimitsConfiguration
    )),
    TWO_FA_VERIFICATION_CODE_SENDING,
    TWO_FA_VERIFICATION_CODE_CHECKING;

    private final Map<LimitLevel, Function<DefaultTenantProfileConfiguration, String>> extractors;
    @Getter
    private final boolean refillRateLimitIntervally;

    LimitedApi(Map<LimitLevel, Function<DefaultTenantProfileConfiguration, String>> extractors) {
        this.extractors = extractors;
        this.refillRateLimitIntervally = false;
    }

    LimitedApi() {
        this.extractors = Collections.emptyMap();
        this.refillRateLimitIntervally = true;
    }

    String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration, LimitLevel limitLevel) {
        return Optional.ofNullable(extractors.get(limitLevel))
                .map(extractor -> extractor.apply(profileConfiguration))
                .orElse(null);
    }

}
