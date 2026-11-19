// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.extension.ExtensionReconcileService;

import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNullElseGet;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbExtensionService extends AbstractTbEntityService implements TbExtensionService {

    private final ExtensionService extensionService;

    @Override
    public Extension save(Extension extension, User user) throws Exception {
        ActionType actionType = extension.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = user.getTenantId();
        extension.setTenantId(tenantId);
        try {
            Extension saved = extensionService.saveExtension(extension);
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            return saved;
        } catch (Exception e) {
            log.error("Error on save extension slug={} error={}", extension.getSlug(), e.getMessage());
            logEntityActionService.logEntityAction(tenantId, requireNonNullElseGet(extension.getId(), () -> emptyId(EntityType.EXTENSION)), extension, actionType, user, e);
            throw e;
        }
    }

    @Override
    public boolean delete(Extension extension, User user) {
        TenantId tenantId = user.getTenantId();
        ExtensionId extensionId = extension.getId();
        try {
            extensionService.deleteExtension(tenantId, extensionId);
            logEntityActionService.logEntityAction(tenantId, extensionId, extension, ActionType.DELETED, user, extensionId.toString());
            return true;
        } catch (Exception e) {
            log.error("Error on delete extension slug={} error={}", extension.getSlug(), e.getMessage());
            logEntityActionService.logEntityAction(tenantId, extensionId, extension, ActionType.DELETED, user, e, extensionId.toString());
            throw e;
        }
    }

    @Override
    public Extension restart(Extension extension, User user) {
        rejectIfDeleting(extension, "restart");
        // A STOPPED extension is scaled to zero, so there is nothing to roll: restartQuietly re-reads the
        // row and returns without acting. Without this guard the caller gets 202 Accepted and a successful
        // audit entry for an operation that was silently discarded, and no hint that /start was the one
        // they wanted. start() rejects its own invalid transition the same way.
        if (extension.getState() == ExtensionState.STOPPED) {
            throw new DataValidationException("Extension is STOPPED and has nothing to restart; start it instead.");
        }
        TenantId tenantId = user.getTenantId();
        ExtensionId extensionId = extension.getId();
        try {
            // Restart neither changes state nor clears lastError; it only rolls the pods.
            tbClusterService.broadcast(ComponentLifecycleMsg.builder()
                    .tenantId(tenantId)
                    .entityId(extensionId)
                    .event(ComponentLifecycleEvent.UPDATED)
                    .info(JacksonUtil.newObjectNode().put("action", ExtensionReconcileService.RESTART_ACTION))
                    .build());
            logEntityActionService.logEntityAction(tenantId, extensionId, extension, ActionType.UPDATED, user, "restart");
            return extension;
        } catch (Exception e) {
            log.error("Error on restart extension slug={} error={}", extension.getSlug(), e.getMessage());
            logEntityActionService.logEntityAction(tenantId, extensionId, extension, ActionType.UPDATED, user, e, "restart");
            throw e;
        }
    }

    @Override
    public Extension stop(Extension extension, User user) {
        rejectIfDeleting(extension, "stop");
        return changeStateAndReconcile(extension, ExtensionState.STOPPED, user, "stop");
    }

    @Override
    public Extension start(Extension extension, User user) {
        rejectIfDeleting(extension, "start");
        if (extension.getState() != ExtensionState.STOPPED) {
            throw new DataValidationException("Extension can only be started from the STOPPED state (current: " + extension.getState() + ").");
        }
        // A STOPPED extension occupies no quota; the capacity check and the flip out of STOPPED are done as
        // one guarded unit, or two concurrent starts both pass on the same headroom.
        return changeStateAndReconcile(extension, ExtensionState.PROVISIONING, user, "start",
                (tenantId, ext) -> extensionService.startWithinQuota(tenantId, ext));
    }

    private Extension changeStateAndReconcile(Extension extension, ExtensionState newState, User user, String action) {
        return changeStateAndReconcile(extension, newState, user, action,
                (tenantId, ext) -> extensionService.updateState(tenantId, ext.getId(), newState, null));
    }

    private Extension changeStateAndReconcile(Extension extension, ExtensionState newState, User user, String action,
                                              BiConsumer<TenantId, Extension> stateChange) {
        TenantId tenantId = user.getTenantId();
        ExtensionId extensionId = extension.getId();
        try {
            stateChange.accept(tenantId, extension);
            Extension updated = extensionService.findExtensionById(tenantId, extensionId);
            // updateState fires no entity-save event, so the reconcile trigger is broadcast explicitly;
            // the partition owner picks it up in ExtensionReconcileService.onComponentLifecycleMsg
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, extensionId, ComponentLifecycleEvent.UPDATED);
            logEntityActionService.logEntityAction(tenantId, extensionId, updated, ActionType.UPDATED, user, action);
            return updated;
        } catch (Exception e) {
            log.error("Error on change state and reconcile extension slug={} error={}", extension.getSlug(), e.getMessage());
            logEntityActionService.logEntityAction(tenantId, extensionId, extension, ActionType.UPDATED, user, e, action);
            throw e;
        }
    }

    private static void rejectIfDeleting(Extension extension, String action) {
        if (extension.getState() == ExtensionState.DELETING) {
            throw new DataValidationException("Cannot " + action + " an extension that is being deleted.");
        }
    }

}
