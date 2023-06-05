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
package org.thingsboard.server.dao.util.limits;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.function.BiFunction;
import java.util.function.Function;

public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit),
    REST_REQUESTS((profileConfiguration, level) -> ((EntityId) level).getEntityType() == EntityType.TENANT ?
            profileConfiguration.getTenantServerRestLimitsConfiguration() :
            profileConfiguration.getCustomerServerRestLimitsConfiguration()),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit),
    CASSANDRA_QUERIES(DefaultTenantProfileConfiguration::getCassandraQueryTenantRateLimitsConfiguration),
    PASSWORD_RESET(true),
    TWO_FA_VERIFICATION_CODE_SEND(true),
    TWO_FA_VERIFICATION_CODE_CHECK(true);

    private final BiFunction<DefaultTenantProfileConfiguration, Object, String> configExtractor;
    @Getter
    private final boolean refillRateLimitIntervally;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor) {
        this((profileConfiguration, level) -> configExtractor.apply(profileConfiguration));
    }

    LimitedApi(BiFunction<DefaultTenantProfileConfiguration, Object, String> configExtractor) {
        this.configExtractor = configExtractor;
        this.refillRateLimitIntervally = false;
    }

    LimitedApi(boolean refillRateLimitIntervally) {
        this.configExtractor = null;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration, Object level) {
        if (configExtractor != null) {
            return configExtractor.apply(profileConfiguration, level);
        } else {
            throw new IllegalArgumentException("No tenant profile config for " + name() + " rate limits");
        }
    }

}
