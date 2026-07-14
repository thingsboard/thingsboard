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
package org.thingsboard.server.service.solutions.data.emulator;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
public class DeviceEmulatorLauncher extends AbstractEmulatorLauncher<Device> {

    @Builder
    public DeviceEmulatorLauncher(Device entity,
                                  EmulatorDefinition emulatorDefinition,
                                  ExecutorService oldTelemetryExecutor,
                                  TbClusterService tbClusterService,
                                  PartitionService partitionService,
                                  TbQueueProducerProvider tbQueueProducerProvider,
                                  TbServiceInfoProvider serviceInfoProvider,
                                  TelemetrySubscriptionService tsSubService) throws Exception {
        super(entity, emulatorDefinition, oldTelemetryExecutor, tbClusterService, partitionService, tbQueueProducerProvider, serviceInfoProvider, tsSubService);
    }

    @Override
    protected void postProcessEntity(Device entity) {
        if (this.emulatorDefinition.getActivityPeriodInMillis() > 0) {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(entity.getTenantId())
                    .entityId(entity.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new LongDataEntry(DefaultDeviceStateService.INACTIVITY_TIMEOUT, this.emulatorDefinition.getActivityPeriodInMillis()))
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void unused) {
                            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, entity.getTenantId(), entity.getId());
                            UUID sessionId = UUID.randomUUID();
                            TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                                    .setSessionInfo(TransportProtos.SessionInfoProto.newBuilder()
                                            .setSessionIdMSB(sessionId.getMostSignificantBits())
                                            .setSessionIdLSB(sessionId.getLeastSignificantBits())
                                            .setDeviceIdMSB(entity.getId().getId().getMostSignificantBits())
                                            .setDeviceIdLSB(entity.getId().getId().getLeastSignificantBits())
                                            .setDeviceProfileIdMSB(entity.getId().getId().getMostSignificantBits())
                                            .setDeviceProfileIdLSB(entity.getId().getId().getLeastSignificantBits())
                                            .setDeviceName(entity.getName())
                                            .setDeviceType(entity.getType())
                                            .setTenantIdMSB(entity.getTenantId().getId().getMostSignificantBits())
                                            .setTenantIdLSB(entity.getTenantId().getId().getLeastSignificantBits())
                                            .setNodeId(serviceInfoProvider.getServiceId())
                                            .build())
                                    .setSubscriptionInfo(TransportProtos.SubscriptionInfoProto.newBuilder().setLastActivityTime(System.currentTimeMillis()).build())
                                    .build();
                            tbQueueProducerProvider.getTbCoreMsgProducer().send(tpi,
                                    new TbProtoQueueMsg<>(entity.getUuidId(),
                                            TransportProtos.ToCoreMsg.newBuilder().setToDeviceActorMsg(msg).build()), EMPTY_CALLBACK
                            );
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                        }
                    })
                    .build());
        }
    }
}
