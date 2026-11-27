// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.sync.ie.RuleNodeUserIdRefs;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.RULE_CHAIN, EntityType.DEVICE, EntityType.ASSET));
    public static final Pattern PROCESSED_CONFIG_FIELDS_PATTERN = Pattern.compile(".*[iI]d.*");

    private final TbRuleChainService tbRuleChainService;
    private final RuleChainService ruleChainService;
    private final RuleNodeDao ruleNodeDao;
    private final UserService userService;

    @Override
    protected void setOwner(TenantId tenantId, RuleChain ruleChain, IdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    @Override
    protected RuleChain findExistingEntity(EntitiesImportCtx ctx, RuleChain ruleChain, IdProvider idProvider) {
        RuleChain existingRuleChain = super.findExistingEntity(ctx, ruleChain, idProvider);
        if (existingRuleChain == null && ctx.isFindExistingByName()) {
            existingRuleChain = ruleChainService.findTenantRuleChainsByTypeAndName(ctx.getTenantId(), ruleChain.getType(), ruleChain.getName()).stream().findFirst().orElse(null);
        }
        return existingRuleChain;
    }

    @Override
    protected RuleChain prepare(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChain old, RuleChainExportData exportData, IdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        List<RuleNode> ruleNodes = Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList());
        if (old != null) {
            List<RuleNodeId> nodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
            List<RuleNode> existing = ruleNodeDao.findByExternalIds(old.getId(), nodeIds);
            existing.forEach(node -> ctx.putInternalId(node.getExternalId(), node.getId()));
            ruleNodes.forEach(node -> {
                node.setRuleChainId(old.getId());
                node.setExternalId(node.getId());
                node.setId((RuleNodeId) ctx.getInternalId(node.getId()));
            });
        } else {
            ruleNodes.forEach(node -> {
                node.setRuleChainId(null);
                node.setExternalId(node.getId());
                node.setId(null);
            });
        }

        Map<UUID, String> userEmails = Optional.ofNullable(exportData.getUserEmails()).orElse(Collections.emptyMap());
        Set<UUID> resolvedUserUuids = new HashSet<>();
        ruleNodes.forEach(ruleNode -> resolveUserIds(ctx, ruleNode, userEmails, resolvedUserUuids));
        ruleNodes.forEach(ruleNode -> replaceIdsRecursively(ctx, idProvider, ruleNode.getConfiguration(),
                Collections.emptySet(), PROCESSED_CONFIG_FIELDS_PATTERN, HINTS, resolvedUserUuids));
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.getInternalId(ruleChainConnectionInfo.getTargetRuleChainId(), false));
                });
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId((RuleNodeId) ctx.getInternalId(ruleChain.getFirstRuleNodeId()));
        }
        return ruleChain;
    }

    /**
     * Users are not imported together with rule chains, and the same user has a different id on every environment.
     * Resolve each user reference by external id, then by id, then by the email recorded at export time.
     * <p>
     * If nothing matches:
     * <ul>
     *     <li>the id belongs to a user of another tenant on this instance: it is cleared to {@link EntityId#NULL_UUID},
     *     so that the node can never act with that user's permissions;</li>
     *     <li>otherwise (the user exists only on another environment) the original id is kept.</li>
     * </ul>
     * All resulting ids are added to {@code resolvedUserUuids} so the generic uuid replacement leaves them alone;
     * otherwise an unknown user id is resolved as TENANT and silently becomes the current tenant id.
     */
    private void resolveUserIds(EntitiesImportCtx ctx, RuleNode ruleNode, Map<UUID, String> userEmails, Set<UUID> resolvedUserUuids) {
        RuleNodeUserIdRefs.replace(ruleNode.getConfiguration(), userUuid -> {
            UUID result = resolveUserId(ctx, ruleNode, userUuid, userEmails.get(userUuid));
            resolvedUserUuids.add(result);
            return result;
        });
    }

    private UUID resolveUserId(EntitiesImportCtx ctx, RuleNode ruleNode, UUID userUuid, String email) {
        if (EntityId.NULL_UUID.equals(userUuid)) {
            return userUuid;
        }
        UserId userId = new UserId(userUuid);
        UserId internalId = findInternalUserId(ctx, userId, email);
        if (internalId != null) {
            return internalId.getId();
        }
        String userInfo = email != null ? userUuid + " (" + email + ")" : userUuid.toString();
        if (entitiesService.findEntityById(userId) != null) {
            log.warn("[{}] Rule node [{}] references user [{}] of another tenant; clearing the user id",
                    ctx.getTenantId(), ruleNode.getName(), userInfo);
            return EntityId.NULL_UUID;
        }
        log.warn("[{}] Rule node [{}] references user [{}] that does not exist on this environment; keeping the original user id",
                ctx.getTenantId(), ruleNode.getName(), userInfo);
        return userUuid;
    }

    private UserId findInternalUserId(EntitiesImportCtx ctx, UserId externalId, String email) {
        EntityId cached = ctx.getInternalId(externalId);
        if (cached instanceof UserId cachedUserId) {
            return cachedUserId;
        }
        TenantId tenantId = ctx.getTenantId();
        User user = entitiesService.findEntityByTenantIdAndExternalId(tenantId, externalId);
        if (user == null) {
            user = entitiesService.findEntityByTenantIdAndId(tenantId, externalId);
        }
        if (user == null && email != null) {
            user = userService.findUserByTenantIdAndEmail(tenantId, email);
        }
        if (user == null) {
            return null;
        }
        ctx.putInternalId(externalId, user.getId());
        return user.getId();
    }

    @Override
    protected RuleChain saveOrUpdate(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChainExportData exportData, IdProvider idProvider, CompareResult compareResult) {
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            exportData.getMetaData().setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), exportData.getMetaData(), tbRuleChainService::updateRuleNodeConfiguration);
            return ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChain.getId());
        } else {
            return ruleChain;
        }
    }

    @Override
    protected boolean isUpdateNeeded(EntitiesImportCtx ctx, RuleChainExportData exportData, RuleChain prepared, RuleChain existing) {
        boolean updateNeeded = super.isUpdateNeeded(ctx, exportData, prepared, existing);
        if (!updateNeeded) {
            RuleChainMetaData newMD = exportData.getMetaData();
            RuleChainMetaData existingMD = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), prepared.getId());
            existingMD.setRuleChainId(null);
            updateNeeded = !newMD.equals(existingMD);
        }
        return updateNeeded;
    }

    @Override
    protected void onEntitySaved(User user, RuleChain savedRuleChain, RuleChain oldRuleChain) {
        entityActionService.logEntityAction(user, savedRuleChain.getId(), savedRuleChain, null,
                oldRuleChain == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected RuleChain deepCopy(RuleChain ruleChain) {
        return new RuleChain(ruleChain);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
