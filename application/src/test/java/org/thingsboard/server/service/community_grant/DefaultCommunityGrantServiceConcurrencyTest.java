// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.google.common.util.concurrent.FluentFuture;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.subscription.TbClusterStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a real {@link CommunityGrantFlowStateStore} over an in-memory settings map, so racing callers genuinely contend.
 */
class DefaultCommunityGrantServiceConcurrencyTest {

    private static final UUID CLUSTER_ID = UUID.randomUUID();
    private static final int THREADS = 8;

    @Test
    void testConcurrentRequestAccessReachesThePortalExactlyOnce() throws Exception {
        InMemoryAdminSettingsService adminSettingsService = new InMemoryAdminSettingsService();
        CommunityGrantFlowStateStore flowStateStore = new CommunityGrantFlowStateStore(adminSettingsService);
        flowStateStore.update(state -> {
            state.setMode(CommunityGrantMode.ONLINE);
            state.setState(CommunityGrantState.ALREADY_REGISTERED);
            return state;
        });

        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getClusterId()).thenReturn(Optional.of(CLUSTER_ID));
        CommunityGrantPortalClient portalClient = mock(CommunityGrantPortalClient.class);
        CommunityGrantPoller poller = mock(CommunityGrantPoller.class);
        CommunityGrantReportRunner reportRunner = mock(CommunityGrantReportRunner.class);
        CommunityGrantOfflineReportStore offlineReportStore =
                new CommunityGrantOfflineReportStore(adminSettingsService);
        DefaultCommunityGrantService service = new DefaultCommunityGrantService(
                tbClusterStore, flowStateStore, portalClient, poller, reportRunner, offlineReportStore);
        ReflectionTestUtils.setField(service, "enabled", true);

        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    try {
                        service.requestAccess();
                    } catch (ThingsboardException expectedForAllButOneCaller) {
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        verify(portalClient, times(1)).requestAccess(eq(CLUSTER_ID));
    }

    private static final class InMemoryAdminSettingsService implements AdminSettingsService {

        private final Map<String, AdminSettings> rows = new ConcurrentHashMap<>();

        @Override
        public AdminSettings findAdminSettingsByKey(TenantId tenantId, String key) {
            return rows.get(key);
        }

        @Override
        public AdminSettings saveAdminSettings(TenantId tenantId, AdminSettings adminSettings) {
            if (adminSettings.getId() == null) {
                adminSettings.setId(new AdminSettingsId(UUID.randomUUID()));
            }
            rows.put(adminSettings.getKey(), adminSettings);
            return adminSettings;
        }

        @Override
        public AdminSettings findAdminSettingsById(TenantId tenantId, AdminSettingsId adminSettingsId) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public AdminSettings findAdminSettingsByTenantIdAndKey(TenantId tenantId, String key) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public PageData<AdminSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public boolean deleteAdminSettingsByTenantIdAndKey(TenantId tenantId, String key) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }

        @Override
        public EntityType getEntityType() {
            throw new UnsupportedOperationException("Not used by CommunityGrantFlowStateStore");
        }
    }

}
