// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.community_grant.CommunityGrantOfflineBundle;
import org.thingsboard.server.service.community_grant.CommunityGrantPlatform;
import org.thingsboard.server.service.community_grant.CommunityGrantService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Hidden
@RestController
@TbCoreComponent
@RequestMapping("/api/communityGrant")
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantController extends BaseController {

    private static final String REPORT_FILE_NAME = "tb-instance-check-report.txt";

    @Value("${community-grant.max-checker-size-bytes:33554432}")
    private long maxCheckerSizeBytes;

    private final CommunityGrantService communityGrantService;
    private final TbClusterStore tbClusterStore;

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/state")
    @ResponseBody
    public CommunityGrantStateInfo getState() {
        return communityGrantService.getStateInfo();
    }

    /** Every failure is audited as a refusal, hence the catch on {@code Exception}. */
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/start")
    @ResponseBody
    public CommunityGrantStateInfo start() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CommunityGrantStateInfo stateInfo;
        try {
            stateInfo = communityGrantService.start();
        } catch (Exception e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                    user.getId(), user, ActionType.COMMUNITY_GRANT_ENROLLMENT, e);
            throw e;
        }
        auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                user.getId(), user, ActionType.COMMUNITY_GRANT_ENROLLMENT, null);
        return stateInfo;
    }

    /**
     * Answers {@code 202} once the run has started on this node; a refusal is audited here, and the run
     * audits its own ending.
     */
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping(value = "/offline/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityGrantStateInfo> runOfflineChecker(@RequestPart("bundle") MultipartFile bundle)
            throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CommunityGrantStateInfo stateInfo;
        try {
            stateInfo = runOfflineChecker(bundle, user);
        } catch (Exception e) {
            auditOfflineRun(user, e);
            throw e;
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(stateInfo);
    }

    private void auditOfflineRun(SecurityUser user, Exception failure) {
        auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                user.getId(), user, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN, failure);
    }

    private CommunityGrantStateInfo runOfflineChecker(MultipartFile bundle, SecurityUser user)
            throws ThingsboardException {
        long maxBundleSize = maxCheckerSizeBytes + CommunityGrantOfflineBundle.MAX_OVERHEAD_BYTES;
        if (bundle.getSize() > maxBundleSize) {
            throw new ThingsboardException("The uploaded enrollment bundle is larger than the configured limit of "
                    + maxBundleSize + " bytes", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        byte[] bundleData;
        try {
            bundleData = bundle.getBytes();
        } catch (IOException e) {
            throw new ThingsboardException("Failed to read the uploaded file", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        CommunityGrantOfflineBundle parsed;
        try {
            parsed = CommunityGrantOfflineBundle.parse(bundleData);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        refuseBundleForAnotherInstallation(parsed);
        refuseBundleForAnotherPlatform(parsed);
        warnIfChallengeExpired(parsed, user);
        return communityGrantService.runOfflineChecker(parsed.checker(), parsed.signature(),
                parsed.checkerFileName(), parsed.checkerInput(), failure -> auditOfflineRun(user, failure));
    }

    /** Skipped when this deployment has no cluster id at all. */
    private void refuseBundleForAnotherInstallation(CommunityGrantOfflineBundle parsed) throws ThingsboardException {
        Optional<UUID> clusterId = tbClusterStore.getClusterId();
        if (clusterId.isPresent() && !clusterId.get().equals(parsed.clusterId())) {
            throw new ThingsboardException("This enrollment bundle was issued for a different installation. "
                    + "Download a bundle for this installation from the License Portal and upload that one.",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void refuseBundleForAnotherPlatform(CommunityGrantOfflineBundle parsed) throws ThingsboardException {
        CommunityGrantPlatform platform;
        try {
            platform = CommunityGrantPlatform.current();
        } catch (RuntimeException e) {
            log.debug("Refusing an enrollment bundle: this server's platform has no instance checker build", e);
            throw new ThingsboardException("This server's operating system and CPU architecture have no "
                    + "instance checker build, so an enrollment bundle cannot be run here.",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!platform.os().equals(parsed.os()) || !platform.arch().equals(parsed.arch())) {
            throw new ThingsboardException("This enrollment bundle is for " + parsed.os() + "/" + parsed.arch()
                    + ", and this server is " + platform.os() + "/" + platform.arch()
                    + ". Download the bundle for this server's platform.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /** Warns, never blocks: the portal is the authoritative clock, and an air-gapped host may have drifted. */
    private void warnIfChallengeExpired(CommunityGrantOfflineBundle parsed, SecurityUser user) {
        if (parsed.isChallengeExpired(System.currentTimeMillis())) {
            log.warn("Running an instance checker from an enrollment bundle whose challenge expired at {} "
                            + "(requested by user {}). The portal will refuse the report it produces; a fresh "
                            + "bundle is the remedy.",
                    Instant.ofEpochMilli(parsed.challengeExpiresAt()), user.getId());
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/requestAccess")
    public void requestAccess() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            communityGrantService.requestAccess();
        } catch (Exception e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                    user.getId(), user, ActionType.COMMUNITY_GRANT_REQUEST_ACCESS, e);
            throw e;
        }
        auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                user.getId(), user, ActionType.COMMUNITY_GRANT_REQUEST_ACCESS, null);
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/offline/handoff")
    @ResponseBody
    public CommunityGrantStateInfo confirmOfflineHandoff() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CommunityGrantStateInfo stateInfo;
        try {
            stateInfo = communityGrantService.confirmOfflineHandoff();
        } catch (Exception e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                    user.getId(), user, ActionType.COMMUNITY_GRANT_ENROLLMENT, e);
            throw e;
        }
        auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(),
                user.getId(), user, ActionType.COMMUNITY_GRANT_ENROLLMENT, null);
        return stateInfo;
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/offline/report")
    public ResponseEntity<Resource> downloadOfflineReport() throws ThingsboardException {
        byte[] report = communityGrantService.getOfflineReport().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + REPORT_FILE_NAME + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(report.length)
                .body(new ByteArrayResource(report));
    }

}
