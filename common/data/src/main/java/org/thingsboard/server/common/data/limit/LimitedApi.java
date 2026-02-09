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
package org.thingsboard.server.common.data.limit;

import lombok.Getter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Optional;
import java.util.function.Function;

@Getter
public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit, "entity version creation", true),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit, "entity version load", true),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit, "notification requests", true),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit, "notification requests per rule", false),
    REST_REQUESTS_PER_TENANT(DefaultTenantProfileConfiguration::getTenantServerRestLimitsConfiguration, "REST API requests", true),
    REST_REQUESTS_PER_CUSTOMER(DefaultTenantProfileConfiguration::getCustomerServerRestLimitsConfiguration, "REST API requests per customer", false),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit, "WS updates per session", true),
    CASSANDRA_WRITE_QUERIES_CORE(DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantCoreRateLimits, "Rest API Cassandra write queries", true),
    CASSANDRA_READ_QUERIES_CORE(DefaultTenantProfileConfiguration::getCassandraReadQueryTenantCoreRateLimits, "Rest API and WS telemetry Cassandra read queries", true),
    CASSANDRA_WRITE_QUERIES_RULE_ENGINE(DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantRuleEngineRateLimits, "Rule Engine telemetry Cassandra write queries", true),
    CASSANDRA_READ_QUERIES_RULE_ENGINE(DefaultTenantProfileConfiguration::getCassandraReadQueryTenantRuleEngineRateLimits, "Rule Engine telemetry Cassandra read queries", true),
    CASSANDRA_READ_QUERIES_MONOLITH(
            RateLimitUtil.merge(
                    DefaultTenantProfileConfiguration::getCassandraReadQueryTenantCoreRateLimits,
                    DefaultTenantProfileConfiguration::getCassandraReadQueryTenantRuleEngineRateLimits), "Monolith telemetry Cassandra read queries", true),
    CASSANDRA_WRITE_QUERIES_MONOLITH(
            RateLimitUtil.merge(
                    DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantCoreRateLimits,
                    DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantRuleEngineRateLimits), "Monolith telemetry Cassandra write queries", true),
    CASSANDRA_QUERIES(null, true), // left for backward compatibility with RateLimitsNotificationInfo
    EDGE_EVENTS(DefaultTenantProfileConfiguration::getEdgeEventRateLimits, "Edge events", true),
    EDGE_EVENTS_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeEventRateLimitsPerEdge, "Edge events per edge", false),
    EDGE_UPLINK_MESSAGES(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimits, "Edge uplink messages", true),
    EDGE_UPLINK_MESSAGES_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimitsPerEdge, "Edge uplink messages per edge", false),
    PASSWORD_RESET(false, true),
    TWO_FA_VERIFICATION_CODE_SEND(false, true),
    TWO_FA_VERIFICATION_CODE_CHECK(false, true),
    TRANSPORT_MESSAGES_PER_TENANT("transport messages", true),
    TRANSPORT_MESSAGES_PER_DEVICE("transport messages per device", false),
    TRANSPORT_MESSAGES_PER_GATEWAY("transport messages per gateway", false),
    TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE("transport messages per gateway device", false),
    EMAILS("emails sending", true),
    WS_SUBSCRIPTIONS("WS subscriptions", false),
    CALCULATED_FIELD_DEBUG_EVENTS("calculated field debug events", true);

    private final Function<DefaultTenantProfileConfiguration, String> configExtractor;
    private final boolean perTenant;
    private final boolean refillRateLimitIntervally;
    private final String label;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant) {
        this(configExtractor, label, perTenant, false);
    }

    LimitedApi(boolean perTenant, boolean refillRateLimitIntervally) {
        this(null, null, perTenant, refillRateLimitIntervally);
    }

    LimitedApi(String label, boolean perTenant) {
        this(null, label, perTenant, false);
    }

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant, boolean refillRateLimitIntervally) {
        this.configExtractor = configExtractor;
        this.label = label;
        this.perTenant = perTenant;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration) {
        return Optional.ofNullable(configExtractor)
                .map(extractor -> extractor.apply(profileConfiguration))
                .orElse(null);
    }

}
