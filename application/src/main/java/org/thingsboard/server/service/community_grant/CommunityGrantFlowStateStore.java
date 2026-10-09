// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

/**
 * Every write goes through {@link #update}, which re-reads under a per-node lock. {@code admin_settings} has no
 * unique index on {@code (tenant_id, key)}, so two creates that both commit leave duplicate rows, and every
 * later read throws until one is deleted by hand.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantFlowStateStore {

    public static final String SETTINGS_KEY = "communityGrant";

    private final AdminSettingsService adminSettingsService;

    private final ReentrantLock updateLock = new ReentrantLock();

    public CommunityGrantFlowState get() {
        try {
            return readOrThrow();
        } catch (IllegalStateException e) {
            // Read-only fallback; a store failure still propagates.
            log.warn("Failed to parse the stored community grant flow state, falling back to the initial state", e);
            return CommunityGrantFlowState.initial();
        }
    }

    /**
     * The updater runs a second time against the winner's document when a create race is lost, so it must be
     * deterministic: read the clock before the call, not inside it.
     */
    public CommunityGrantFlowState update(UnaryOperator<CommunityGrantFlowState> updater) {
        updateLock.lock();
        try {
            AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
            JsonNode stored = settings != null ? settings.getJsonValue() : null;
            CommunityGrantFlowState updated = updater.apply(parseOrThrow(settings));
            JsonNode serialized = JacksonUtil.valueToTree(updated);
            // Every poll runs an updater; an unchanged document is not written back.
            if (serialized.equals(stored)) {
                return updated;
            }
            if (settings == null) {
                settings = new AdminSettings();
                settings.setTenantId(TenantId.SYS_TENANT_ID);
                settings.setKey(SETTINGS_KEY);
            }
            settings.setJsonValue(serialized);
            try {
                adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
                return updated;
            } catch (DataValidationException e) {
                if (settings.getId() != null) {
                    throw e;
                }
                // Lost a create race to another node: re-apply the change to the winner's row.
                AdminSettings winner = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
                if (winner == null) {
                    throw e;
                }
                CommunityGrantFlowState reapplied;
                try {
                    reapplied = updater.apply(parseOrThrow(winner));
                } catch (RuntimeException retryFailure) {
                    retryFailure.addSuppressed(e);
                    throw retryFailure;
                }
                winner.setJsonValue(JacksonUtil.valueToTree(reapplied));
                adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, winner);
                return reapplied;
            }
        } finally {
            updateLock.unlock();
        }
    }

    private CommunityGrantFlowState readOrThrow() {
        return parseOrThrow(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY));
    }

    private CommunityGrantFlowState parseOrThrow(AdminSettings settings) {
        if (settings == null || settings.getJsonValue() == null || settings.getJsonValue().isNull()) {
            return CommunityGrantFlowState.initial();
        }
        CommunityGrantFlowState flowState;
        try {
            flowState = JacksonUtil.treeToValue(settings.getJsonValue(), CommunityGrantFlowState.class);
        } catch (Exception e) {
            // The cause is dropped: it carries the whole stored document, claim token included.
            throw new IllegalStateException("Stored community grant flow state at admin_settings[" + SETTINGS_KEY
                    + "] could not be parsed: " + e.getClass().getSimpleName());
        }
        if (flowState == null || flowState.getState() == null) {
            return CommunityGrantFlowState.initial();
        }
        if (flowState.getMode() == null) {
            flowState.setMode(CommunityGrantMode.ONLINE);
        }
        return flowState;
    }

}
