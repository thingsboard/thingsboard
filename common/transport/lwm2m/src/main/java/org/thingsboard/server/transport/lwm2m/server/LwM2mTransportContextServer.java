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
package org.thingsboard.server.transport.lwm2m.server;
/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.lwm2m.LwM2MTransportConfigServer;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourcesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourcesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LOG_LW2M_TELEMETRY;

@Slf4j
@Component
@TbLwM2mTransportComponent
public class LwM2mTransportContextServer extends TransportContext {


    private final LwM2MTransportConfigServer lwM2MTransportConfigServer;

    private final TransportService transportService;

    private List<ObjectModel> modelsValueServer;

    @Getter
    private final LwM2MJsonAdaptor adaptor;

    public LwM2mTransportContextServer(LwM2MTransportConfigServer lwM2MTransportConfigServer, TransportService transportService, LwM2MJsonAdaptor adaptor) {
        this.lwM2MTransportConfigServer = lwM2MTransportConfigServer;
        this.transportService = transportService;
        this.adaptor = adaptor;
    }

    public LwM2MTransportConfigServer getLwM2MTransportConfigServer() {
        return this.lwM2MTransportConfigServer;
    }

    /**
     * Sent to Thingsboard Attribute || Telemetry
     *
     * @param msg   - JsonObject: [{name: value}]
     * @return - dummy
     */
    private <T> TransportServiceCallback<Void> getPubAckCallbackSentAttrTelemetry(final T msg) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("Success to publish msg: {}, dummy: {}", msg, dummy);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", msg, e);
            }
        };
    }

    public void sentParametersOnThingsboard(JsonElement msg, String topicName, SessionInfoProto sessionInfo) {
        try {
            if (topicName.equals(LwM2mTransportHandler.DEVICE_ATTRIBUTES_TOPIC)) {
                PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                TransportServiceCallback call = this.getPubAckCallbackSentAttrTelemetry(postAttributeMsg);
                transportService.process(sessionInfo, postAttributeMsg, this.getPubAckCallbackSentAttrTelemetry(call));
            } else if (topicName.equals(LwM2mTransportHandler.DEVICE_TELEMETRY_TOPIC)) {
                PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
                TransportServiceCallback call = this.getPubAckCallbackSentAttrTelemetry(postTelemetryMsg);
                transportService.process(sessionInfo, postTelemetryMsg, this.getPubAckCallbackSentAttrTelemetry(call));
            }
        } catch (AdaptorException e) {
            log.error("[{}] Failed to process publish msg [{}]", topicName, e);
            log.info("[{}] Closing current session due to invalid publish", topicName);
        }
    }

    public JsonObject getTelemetryMsgObject(String logMsg) {
        JsonObject telemetries = new JsonObject();
        telemetries.addProperty(LOG_LW2M_TELEMETRY, logMsg);
        return telemetries;
    }

    /**
     * @return - sessionInfo after access connect client
     */
    public SessionInfoProto getValidateSessionInfo(ValidateDeviceCredentialsResponseMsg msg, long mostSignificantBits, long leastSignificantBits) {
        return SessionInfoProto.newBuilder()
                .setNodeId(this.getNodeId())
                .setSessionIdMSB(mostSignificantBits)
                .setSessionIdLSB(leastSignificantBits)
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceIdMSB())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceIdLSB())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantIdMSB())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantIdLSB())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileIdLSB())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileIdMSB())
                .build();
    }




    /**
     * ResourcesRequestMsg
     *
     * @param resourceType
     * @return
     */
    public GetResourcesResponseMsg getResourceTenant (UUID tenantId, String resourceType) {
        CountDownLatch latch = new CountDownLatch(1);
        GetResourcesResponseMsg responseMsg =
                this.getTransportService()
                        .getResources(GetResourcesRequestMsg.newBuilder()
                                .setResourceType(resourceType)
                                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                                .setTenantIdMSB(tenantId.getMostSignificantBits())
                                .build());
        latch.countDown();
        try {
            latch.await(this.getLwM2MTransportConfigServer().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }

        return responseMsg;
    }

    public GetResourcesResponseMsg getResourceTenantProcess (UUID tenantId, String resourceType) {
        CountDownLatch latch = new CountDownLatch(2);
        final GetResourcesResponseMsg[] responseMsg = {null};
        this.getTransportService().process(GetResourcesRequestMsg.newBuilder()
                        .setResourceType(resourceType)
                        .setTenantIdLSB(tenantId.getLeastSignificantBits())
                        .setTenantIdMSB(tenantId.getMostSignificantBits())
                        .build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(GetResourcesResponseMsg msg) { responseMsg[0] = msg;
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] [{}] Failed to process credentials ", tenantId, e);
                        latch.countDown();
                    }
                });
        try {
            latch.await(this.getLwM2MTransportConfigServer().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }
        return responseMsg[0];
    }
}
