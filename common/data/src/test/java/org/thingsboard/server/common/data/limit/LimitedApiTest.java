// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.limit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LimitedApiTest {

    private DefaultTenantProfileConfiguration config;

    @BeforeEach
    void setUp() {
        config = mock(DefaultTenantProfileConfiguration.class);
    }

    @Test
    void testCorrectConfigExtractorsUsed() {
        Map<LimitedApi, Runnable> verifierMap = Map.ofEntries(
                Map.entry(LimitedApi.ENTITY_EXPORT, () ->
                        verify(config).getTenantEntityExportRateLimit()),
                Map.entry(LimitedApi.ENTITY_IMPORT, () ->
                        verify(config).getTenantEntityImportRateLimit()),
                Map.entry(LimitedApi.NOTIFICATION_REQUESTS, () ->
                        verify(config).getTenantNotificationRequestsRateLimit()),
                Map.entry(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, () ->
                        verify(config).getTenantNotificationRequestsPerRuleRateLimit()),
                Map.entry(LimitedApi.REST_REQUESTS_PER_TENANT, () ->
                        verify(config).getTenantServerRestLimitsConfiguration()),
                Map.entry(LimitedApi.REST_REQUESTS_PER_CUSTOMER, () ->
                        verify(config).getCustomerServerRestLimitsConfiguration()),
                Map.entry(LimitedApi.WS_UPDATES_PER_SESSION, () ->
                        verify(config).getWsUpdatesPerSessionRateLimit()),
                Map.entry(LimitedApi.CASSANDRA_WRITE_QUERIES_CORE, () ->
                        verify(config).getCassandraWriteQueryTenantCoreRateLimits()),
                Map.entry(LimitedApi.CASSANDRA_READ_QUERIES_CORE, () ->
                        verify(config).getCassandraReadQueryTenantCoreRateLimits()),
                Map.entry(LimitedApi.CASSANDRA_WRITE_QUERIES_RULE_ENGINE, () ->
                        verify(config).getCassandraWriteQueryTenantRuleEngineRateLimits()),
                Map.entry(LimitedApi.CASSANDRA_READ_QUERIES_RULE_ENGINE, () ->
                        verify(config).getCassandraReadQueryTenantRuleEngineRateLimits()),
                Map.entry(LimitedApi.CASSANDRA_READ_QUERIES_MONOLITH, () -> {
                    verify(config).getCassandraReadQueryTenantCoreRateLimits();
                    verify(config).getCassandraReadQueryTenantRuleEngineRateLimits();
                }),
                Map.entry(LimitedApi.CASSANDRA_WRITE_QUERIES_MONOLITH, () -> {
                    verify(config).getCassandraWriteQueryTenantCoreRateLimits();
                    verify(config).getCassandraWriteQueryTenantRuleEngineRateLimits();
                }),
                Map.entry(LimitedApi.EDGE_EVENTS, () ->
                        verify(config).getEdgeEventRateLimits()),
                Map.entry(LimitedApi.EDGE_EVENTS_PER_EDGE, () ->
                        verify(config).getEdgeEventRateLimitsPerEdge()),
                Map.entry(LimitedApi.EDGE_UPLINK_MESSAGES, () ->
                        verify(config).getEdgeUplinkMessagesRateLimits()),
                Map.entry(LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE, () ->
                        verify(config).getEdgeUplinkMessagesRateLimitsPerEdge())
        );

        Set<LimitedApi> expected = verifierMap.keySet();
        Set<LimitedApi> actual = Arrays.stream(LimitedApi.values())
                .filter(api -> api.getConfigExtractor() != null)
                .collect(Collectors.toSet());

        assertThat(expected)
                .as("Verifier map should cover all LimitedApis with extractors")
                .containsExactlyInAnyOrderElementsOf(actual);

        for (Map.Entry<LimitedApi, Runnable> entry : verifierMap.entrySet()) {
            LimitedApi api = entry.getKey();
            api.getLimitConfig(config);
            entry.getValue().run();
            clearInvocations(config);
        }
    }

}
