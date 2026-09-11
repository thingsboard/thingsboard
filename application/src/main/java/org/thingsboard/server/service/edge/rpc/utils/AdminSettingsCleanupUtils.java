// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.utils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public final class AdminSettingsCleanupUtils {

    private static final String ADMIN_SETTINGS_CLEANUP_COMPLETED_ATTR_KEY = "adminSettingsCleanupCompleted";

    /**
     * Sends the one-time admin settings cleanup pack to edges that were provisioned before admin settings sync was
     * removed, then runs {@code continuation}. Edges on {@code cleanupUpToVersion} or newer never received the
     * settings, so they are skipped. The completion flag is stored as a server attribute on the edge, so the cleanup
     * is sent once; if the pack is interrupted the flag is not set and the cleanup is retried on the next sync.
     *
     * @param downlinkMsgsPackSender sends a downlink pack, returning whether the send was interrupted
     * @param continuation           always invoked, whether or not the cleanup ran or failed
     */
    public static void sendOneTimeAdminSettingsCleanupIfNeeded(EdgeContextComponent ctx, Edge edge, EdgeVersion edgeVersion,
                                                              EdgeVersion cleanupUpToVersion,
                                                              Function<List<DownlinkMsg>, ListenableFuture<Boolean>> downlinkMsgsPackSender,
                                                              Runnable continuation) {
        if (!EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, cleanupUpToVersion)) {
            continuation.run();
            return;
        }
        TenantId tenantId = edge.getTenantId();
        try {
            ListenableFuture<Optional<AttributeKvEntry>> isCleanupCompletedAttrFuture = ctx.getAttributesService()
                    .find(tenantId, edge.getId(), AttributeScope.SERVER_SCOPE, ADMIN_SETTINGS_CLEANUP_COMPLETED_ATTR_KEY);

            ListenableFuture<Boolean> cleanupFuture = Futures.transformAsync(
                    isCleanupCompletedAttrFuture,
                    attr -> {
                        boolean alreadyCompleted = attr.isPresent() && attr.flatMap(AttributeKvEntry::getBooleanValue).orElse(false);
                        if (alreadyCompleted) {
                            return Futures.immediateFuture(false);
                        }
                        List<DownlinkMsg> cleanupMsgs = ctx.getAdminSettingsProcessor().convertAdminSettingsCleanupToDownlinks(edge);
                        if (cleanupMsgs.isEmpty()) {
                            return Futures.immediateFuture(true);
                        }
                        // send the cleanup pack; mark completed only if it was not interrupted, so it retries on next sync otherwise
                        return Futures.transform(downlinkMsgsPackSender.apply(cleanupMsgs),
                                isInterrupted -> !Boolean.TRUE.equals(isInterrupted), ctx.getGrpcCallbackExecutorService());
                    }, ctx.getGrpcCallbackExecutorService());

            Futures.addCallback(cleanupFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Boolean markCompleted) {
                    if (Boolean.TRUE.equals(markCompleted)) {
                        markAdminSettingsCleanupCompleted(ctx, edge);
                    }
                    continuation.run();
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to run one-time admin settings cleanup for edge", tenantId, edge.getId(), t);
                    continuation.run();
                }
            }, ctx.getGrpcCallbackExecutorService());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to start admin settings cleanup", tenantId, edge.getId(), e);
            continuation.run();
        }
    }

    private static void markAdminSettingsCleanupCompleted(EdgeContextComponent ctx, Edge edge) {
        AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(
                new BooleanDataEntry(ADMIN_SETTINGS_CLEANUP_COMPLETED_ATTR_KEY, true), System.currentTimeMillis());
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), AttributeScope.SERVER_SCOPE, attributeKvEntry);
    }

}
