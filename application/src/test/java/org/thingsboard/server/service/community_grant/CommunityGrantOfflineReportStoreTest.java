// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityGrantOfflineReportStoreTest {

    private static final String REPORT = "-----BEGIN TB INSTANCE CHECK-----\nbody";

    @Mock
    private AdminSettingsService adminSettingsService;

    private CommunityGrantOfflineReportStore store;

    private AdminSettings stored;

    /** A counter, so two writes microseconds apart still get distinct instants. */
    private final AtomicLong clock = new AtomicLong();

    @BeforeEach
    void setUp() {
        // Hands out a copy, so a save that threw cannot change what the next read sees.
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID,
                CommunityGrantOfflineReportStore.SETTINGS_KEY))
                .thenAnswer(invocation -> stored == null ? null : new AdminSettings(stored));
        saveSucceeds();
        store = node();
    }

    /** A restart or a second cluster node over the same settings. */
    private CommunityGrantOfflineReportStore node() {
        CommunityGrantOfflineReportStore node = new CommunityGrantOfflineReportStore(adminSettingsService);
        node.setClock(clock::incrementAndGet);
        return node;
    }

    private void saveSucceeds() {
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any()))
                .thenAnswer(invocation -> {
                    AdminSettings saved = invocation.getArgument(1);
                    return stored = new AdminSettings(saved);
                });
    }

    private void saveFails() {
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any()))
                .thenThrow(new IllegalStateException("connection refused"));
    }

    @Test
    void testNoReportIsStoredBeforeAnyRun() {
        assertThat(store.get()).isEmpty();
    }

    @Test
    void testAReportSurvivesTheNodeThatProducedIt() {
        store.put(REPORT);

        assertThat(node().get()).contains(REPORT);
    }

    @Test
    void testANewRunReplacesThePreviousReport() {
        store.put(REPORT);
        store.put(REPORT + " (second run)");

        assertThat(node().get()).contains(REPORT + " (second run)");
    }

    @Test
    void testANewerReportFromAnotherNodeSupersedesThisNodesOwn() {
        store.put(REPORT);

        node().put(REPORT + " (another node)");

        assertThat(store.get()).contains(REPORT + " (another node)");
    }

    @Test
    void testClearRemovesTheStoredReport() {
        store.put(REPORT);
        store.clear();

        assertThat(store.get()).isEmpty();
        assertThat(node().get()).isEmpty();
    }

    @Test
    void testAClearOnAnotherNodeRetiresTheCopyThisNodeProduced() {
        store.put(REPORT);

        node().clear();

        assertThat(store.get()).isEmpty();
    }

    @Test
    void testAReportIsStillServedWhenTheDurableWriteFails() {
        // An existing row, so the failed write is not hidden behind the empty-read fallback.
        store.put(REPORT + " (earlier run)");
        saveFails();

        store.put(REPORT);

        assertThat(store.get()).contains(REPORT);
    }

    @Test
    void testAReportIsStillServedWhenTheDurableWriteFailsAfterAClear() {
        store.clear();
        saveFails();

        store.put(REPORT);

        assertThat(store.get()).contains(REPORT);
    }

    @Test
    void testAFailedReadFallsBackToThisNodesOwnCopy() {
        store.put(REPORT);
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID,
                CommunityGrantOfflineReportStore.SETTINGS_KEY))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThat(store.get()).contains(REPORT);
    }

    @Test
    void testAReportIsStillServedWhileTheDatabaseIsUnreachableAltogether() {
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID,
                CommunityGrantOfflineReportStore.SETTINGS_KEY))
                .thenThrow(new IllegalStateException("connection refused"));
        saveFails();

        store.put(REPORT);

        assertThat(store.get()).contains(REPORT);
    }

    @Test
    void testAClearOnAnotherNodeAfterAFailedWriteRetiresTheCopy() {
        saveFails();
        store.put(REPORT);

        saveSucceeds();
        node().clear();

        assertThat(store.get()).isEmpty();
    }

    @Test
    void testANewerReportFromAnotherNodeAfterAFailedWriteSupersedesTheCopy() {
        saveFails();
        store.put(REPORT);

        saveSucceeds();
        node().put(REPORT + " (another node)");

        assertThat(store.get()).contains(REPORT + " (another node)");
    }

    @Test
    void testASupersededCopyIsNotRevivedByAnOutOfOrderWrite() {
        saveFails();
        store.put(REPORT);
        saveSucceeds();
        node().put(REPORT + " (another node)");
        assertThat(store.get()).contains(REPORT + " (another node)");

        CommunityGrantOfflineReportStore nodeWithABehindClock = node();
        nodeWithABehindClock.setClock(() -> 0L);
        nodeWithABehindClock.clear();

        assertThat(store.get()).isEmpty();
    }

    @Test
    void testAFailedClearDoesNotResurrectTheStoredReport() {
        store.put(REPORT);
        saveFails();

        store.clear();

        assertThat(store.get()).isEmpty();
    }

    @Test
    void testAFailedReadWithNothingLocalPropagates() {
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID,
                CommunityGrantOfflineReportStore.SETTINGS_KEY))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> store.get()).isInstanceOf(IllegalStateException.class);
    }

}
