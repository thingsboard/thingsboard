/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.importing.AbstractBulkImportService;
import org.thingsboard.server.service.importing.BulkImportColumnType;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.ImportedEntityInfo;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
public class EdgeBulkImportService extends AbstractBulkImportService<Edge> {
    private final EdgeService edgeService;

    public EdgeBulkImportService(TelemetrySubscriptionService tsSubscriptionService, TbTenantProfileCache tenantProfileCache,
                                 AccessControlService accessControlService, AccessValidator accessValidator,
                                 EntityActionService entityActionService, TbClusterService clusterService, EdgeService edgeService) {
        super(tsSubscriptionService, tenantProfileCache, accessControlService, accessValidator, entityActionService, clusterService);
        this.edgeService = edgeService;
    }

    @Override
    protected ImportedEntityInfo<Edge> saveEntity(BulkImportRequest importRequest, Map<BulkImportColumnType, String> fields, SecurityUser user) {
        ImportedEntityInfo<Edge> importedEntityInfo = new ImportedEntityInfo<>();

        Edge edge = new Edge();
        edge.setTenantId(user.getTenantId());
        setEdgeFields(edge, fields);

        Edge existingEdge = edgeService.findEdgeByTenantIdAndName(user.getTenantId(), edge.getName());
        if (existingEdge != null && importRequest.getMapping().getUpdate()) {
            importedEntityInfo.setOldEntity(new Edge(existingEdge));
            importedEntityInfo.setUpdated(true);
            existingEdge.update(edge);
            edge = existingEdge;
        }
        edge = edgeService.saveEdge(edge, true);

        importedEntityInfo.setEntity(edge);
        return importedEntityInfo;
    }

    private void setEdgeFields(Edge edge, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(edge.getAdditionalInfo()).orElseGet(JacksonUtil::newObjectNode);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    edge.setName(value);
                    break;
                case TYPE:
                    edge.setType(value);
                    break;
                case LABEL:
                    edge.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case EDGE_LICENSE_KEY:
                    edge.setEdgeLicenseKey(value);
                    break;
                case CLOUD_ENDPOINT:
                    edge.setCloudEndpoint(value);
                    break;
                case ROUTING_KEY:
                    edge.setRoutingKey(value);
                    break;
                case SECRET:
                    edge.setSecret(value);
                    break;
            }
        });
        edge.setAdditionalInfo(additionalInfo);
    }

}
