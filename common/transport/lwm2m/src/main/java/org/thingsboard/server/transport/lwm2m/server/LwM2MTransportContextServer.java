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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.lwm2m.LwM2MTransportConfigServer;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;

import javax.annotation.PostConstruct;

import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.LOG_LW2M_TELEMETRY;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportContextServer extends TransportContext {

    private LwM2MTransportConfigServer ctxServer;

    @Autowired
    protected LwM2MTransportConfigServer lwM2MTransportConfigServer;

    @Autowired
    private TransportService transportService;

    @Getter
    @Autowired
    private LwM2MJsonAdaptor adaptor;

    @Autowired
    LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    @PostConstruct
    public void init() {
        this.ctxServer = lwM2MTransportConfigServer;
    }

    public LwM2MTransportConfigServer getCtxServer() {
        return this.ctxServer;
    }

    /**
     * Sent to Thingsboard Attribute || Telemetry
     *
     * @param msg   - JsonObject: [{name: value}]
     * @return - dummy
     */
    private <T> TransportServiceCallback<Void> getPubAckCallbackSentAttrTelemetry(final T msg) {
        return new TransportServiceCallback<Void>() {
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

    public void sentParametersOnThingsboard(JsonElement msg, String topicName, TransportProtos.SessionInfoProto sessionInfo) {
        try {
            if (topicName.equals(LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                TransportServiceCallback call = this.getPubAckCallbackSentAttrTelemetry(postAttributeMsg);
                transportService.process(sessionInfo, postAttributeMsg, this.getPubAckCallbackSentAttrTelemetry(call));
            } else if (topicName.equals(LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
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
    public TransportProtos.SessionInfoProto getValidateSessionInfo(TransportProtos.ValidateDeviceCredentialsResponseMsg msg, long mostSignificantBits, long leastSignificantBits) {
        return TransportProtos.SessionInfoProto.newBuilder()
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

}
