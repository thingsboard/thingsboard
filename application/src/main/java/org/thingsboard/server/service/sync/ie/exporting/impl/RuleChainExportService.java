// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.ie.RuleNodeUserIdRefs;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.thingsboard.server.service.sync.ie.importing.impl.RuleChainImportService.PROCESSED_CONFIG_FIELDS_PATTERN;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainExportService extends BaseEntityExportService<RuleChainId, RuleChain, RuleChainExportData> {

    private final RuleChainService ruleChainService;
    private final UserService userService;

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, RuleChain ruleChain, RuleChainExportData exportData) {
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), ruleChain.getId());
        Map<UUID, String> userEmails = new TreeMap<>();
        Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList())
                .forEach(ruleNode -> {
                    ruleNode.setRuleChainId(null);
                    ctx.putExternalId(ruleNode.getId(), ruleNode.getExternalId());
                    ruleNode.setId(ctx.getExternalId(ruleNode.getId()));
                    ruleNode.setCreatedTime(0);
                    ruleNode.setExternalId(null);
                    collectUserEmails(ctx, ruleNode.getConfiguration(), userEmails);
                    replaceUuidsRecursively(ctx, ruleNode.getConfiguration(), Collections.emptySet(), PROCESSED_CONFIG_FIELDS_PATTERN);
                });
        exportData.setUserEmails(userEmails.isEmpty() ? null : userEmails);
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(getExternalIdOrElseInternal(ctx, ruleChainConnectionInfo.getTargetRuleChainId()));
                });
        exportData.setMetaData(metaData);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId(ctx.getExternalId(ruleChain.getFirstRuleNodeId()));
        }
    }

    private void collectUserEmails(EntitiesExportCtx<?> ctx, JsonNode configuration, Map<UUID, String> userEmails) {
        RuleNodeUserIdRefs.replace(configuration, userUuid -> {
            if (!EntityId.NULL_UUID.equals(userUuid)) {
                User user = userService.findUserById(ctx.getTenantId(), new UserId(userUuid));
                if (user != null && ctx.getTenantId().equals(user.getTenantId())) {
                    userEmails.put(getExternalIdOrElseInternal(ctx, user.getId()).getId(), user.getEmail());
                }
            }
            return userUuid;
        });
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.RULE_CHAIN);
    }

}
