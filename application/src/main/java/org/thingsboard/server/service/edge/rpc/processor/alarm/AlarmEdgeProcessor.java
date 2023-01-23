/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class AlarmEdgeProcessor extends BaseAlarmProcessor {

    public DownlinkMsg convertAlarmEventToDownlink(EdgeEvent edgeEvent) {
        AlarmUpdateMsg alarmUpdateMsg =
                convertAlarmEventToAlarmMsg(edgeEvent.getTenantId(), edgeEvent.getEntityId(), edgeEvent.getAction(), edgeEvent.getBody());
        if (alarmUpdateMsg != null) {
            return DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAlarmUpdateMsg(alarmUpdateMsg)
                    .build();
        }
        return null;
    }

    public ListenableFuture<Void> processAlarmNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case DELETED:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                Alarm deletedAlarm = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), Alarm.class);
                return saveEdgeEvent(tenantId, edgeId, EdgeEventType.ALARM, actionType, alarmId, JacksonUtil.OBJECT_MAPPER.valueToTree(deletedAlarm));
            default:
                ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
                return Futures.transformAsync(alarmFuture, alarm -> {
                    if (alarm == null) {
                        return Futures.immediateFuture(null);
                    }
                    EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(alarm.getOriginator().getEntityType());
                    if (type == null) {
                        return Futures.immediateFuture(null);
                    }
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<EdgeId> pageData;
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    do {
                        pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, alarm.getOriginator(), pageLink);
                        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                            for (EdgeId relatedEdgeId : pageData.getData()) {
                                futures.add(saveEdgeEvent(tenantId,
                                        relatedEdgeId,
                                        EdgeEventType.ALARM,
                                        EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                                        alarmId,
                                        null));
                            }
                            if (pageData.hasNext()) {
                                pageLink = pageLink.nextPageLink();
                            }
                        }
                    } while (pageData != null && pageData.hasNext());
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
        }
    }

}
