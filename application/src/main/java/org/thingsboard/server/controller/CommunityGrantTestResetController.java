// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.community_grant.CommunityGrantFlowStateStore;
import org.thingsboard.server.service.community_grant.CommunityGrantOfflineReportStore;
import org.thingsboard.server.service.community_grant.CommunityGrantPoller;
import org.thingsboard.server.service.community_grant.CommunityGrantService;

import java.util.UUID;

/** FIXME: delete this whole class before merge. Test-only reset that makes the enrollment flow re-runnable. */
@RestController
@TbCoreComponent
@RequestMapping("/api/communityGrant/test")
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantTestResetController extends BaseController {

    private final JdbcTemplate jdbcTemplate;
    private final TbClusterStore tbClusterStore;
    private final AdminSettingsService adminSettingsService;
    private final CommunityGrantOfflineReportStore offlineReportStore;
    private final CommunityGrantPoller poller;
    private final CommunityGrantService communityGrantService;

    // The poller is stopped first, so a tick in flight cannot write the enrollment back after the reset.
    @ApiOperation(value = "Reset the community grant enrollment (testing only)",
            notes = "Discards this deployment's claim token, its persisted community grant flow and the last "
                    + "offline report, and mints a new cluster id so the portal sees an installation it has "
                    + "never registered. Available for users with 'SYS_ADMIN' authority.")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/reset")
    @ResponseBody
    public CommunityGrantTestResetResult reset(@RequestParam(defaultValue = "true") boolean newClusterId) {
        poller.disarm();
        tbClusterStore.forceClearLicenseClaimToken();
        offlineReportStore.clear();
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID,
                CommunityGrantFlowStateStore.SETTINGS_KEY);
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID,
                CommunityGrantOfflineReportStore.SETTINGS_KEY);
        UUID clusterId = newClusterId ? mintClusterId() : tbClusterStore.getClusterId().orElse(null);
        log.warn("Community grant enrollment reset by the test endpoint. Cluster id is now {}", clusterId);
        return new CommunityGrantTestResetResult(clusterId, communityGrantService.getStateInfo());
    }

    private UUID mintClusterId() {
        UUID clusterId = UUID.randomUUID();
        if (jdbcTemplate.update("UPDATE tb_cluster SET cluster_id = ?::uuid", clusterId.toString()) == 0) {
            jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?::uuid)", clusterId.toString());
        }
        return clusterId;
    }

    public record CommunityGrantTestResetResult(UUID clusterId, CommunityGrantStateInfo state) {
    }

}
