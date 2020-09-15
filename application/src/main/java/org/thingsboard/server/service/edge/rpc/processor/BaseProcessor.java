/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.state.DeviceStateService;

@Slf4j
public abstract class BaseProcessor {

    protected static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    protected TbRuleEngineDeviceRpcService tbDeviceRpcService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected DeviceStateService deviceStateService;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutorService;

    protected ListenableFuture<EdgeEvent> saveEdgeEvent(TenantId tenantId,
                                                        EdgeId edgeId,
                                                        EdgeEventType edgeEventType,
                                                        ActionType edgeEventAction,
                                                        EntityId entityId,
                                                        JsonNode entityBody) {
        log.debug("Pushing event to edge queue. tenantId [{}], edgeId [{}], edgeEventType[{}], " +
                        "edgeEventAction [{}], entityId [{}], entityBody [{}]",
                tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setEntityBody(entityBody);
        return edgeEventService.saveAsync(edgeEvent);
    }
}
