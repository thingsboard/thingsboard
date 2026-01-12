/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.importing.csv.AbstractBulkImportService;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeBulkImportService extends AbstractBulkImportService<Edge> {
    private final EdgeService edgeService;
    private final TbEdgeService tbEdgeService;
    private final RuleChainService ruleChainService;

    @Override
    protected void setEntityFields(Edge entity, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case ROUTING_KEY:
                    entity.setRoutingKey(value);
                    break;
                case SECRET:
                    entity.setSecret(value);
                    break;
            }
        });
        entity.setAdditionalInfo(additionalInfo);
    }

    @SneakyThrows
    @Override
    protected Edge saveEntity(SecurityUser user, Edge entity, Map<BulkImportColumnType, String> fields) {
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        return tbEdgeService.save(entity, edgeTemplateRootRuleChain, user);
    }

    @Override
    protected Edge findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(edgeService.findEdgeByTenantIdAndName(tenantId, name))
                .orElseGet(Edge::new);
    }

    @Override
    protected void setOwners(Edge entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    @Override
    protected EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
