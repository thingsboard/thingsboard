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
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.BOOLEAN_V;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mTransportServerHelper {

    private final LwM2mTransportContext context;
    private final LwM2MJsonAdaptor adaptor;
    private final AtomicInteger atomicTs = new AtomicInteger(0);


    public long getTS() {
        int addTs =  atomicTs.getAndIncrement() >= 1000 ? atomicTs.getAndSet(0) : atomicTs.get();
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) * 1000L +  addTs;
    }

    /**
     * send to Thingsboard Attribute || Telemetry
     *
     * @param msg - JsonObject: [{name: value}]
     * @return - dummyWriteReplace {\"targetIdVer\":\"/19_1.0/0/0\",\"value\":0082}
     */
    private <T> TransportServiceCallback<Void> getPubAckCallbackSendAttrTelemetry(final T msg) {
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

    public void sendParametersOnThingsboardAttribute(List<TransportProtos.KeyValueProto> result, SessionInfoProto sessionInfo) {
        PostAttributeMsg.Builder request = PostAttributeMsg.newBuilder();
        request.addAllKv(result);
        PostAttributeMsg postAttributeMsg = request.build();
        TransportServiceCallback call = this.getPubAckCallbackSendAttrTelemetry(postAttributeMsg);
        context.getTransportService().process(sessionInfo, postAttributeMsg, this.getPubAckCallbackSendAttrTelemetry(call));
    }

    public void sendParametersOnThingsboardTelemetry(List<TransportProtos.KeyValueProto> result, SessionInfoProto sessionInfo) {
        PostTelemetryMsg.Builder request = PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.setTs(this.getTS());
        builder.addAllKv(result);
        request.addTsKvList(builder.build());
        PostTelemetryMsg postTelemetryMsg = request.build();
        TransportServiceCallback call = this.getPubAckCallbackSendAttrTelemetry(postTelemetryMsg);
        context.getTransportService().process(sessionInfo, postTelemetryMsg, this.getPubAckCallbackSendAttrTelemetry(call));
    }

    /**
     * @return - sessionInfo after access connect client
     */
    public SessionInfoProto getValidateSessionInfo(ValidateDeviceCredentialsResponse msg, long mostSignificantBits, long leastSignificantBits) {
        return SessionInfoProto.newBuilder()
                .setNodeId(context.getNodeId())
                .setSessionIdMSB(mostSignificantBits)
                .setSessionIdLSB(leastSignificantBits)
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .build();
    }

    public ObjectModel parseFromXmlToObjectModel(byte[] xmlByte, String streamName, DefaultDDFFileValidator ddfValidator) {
        try {
            DDFFileParser ddfFileParser = new DDFFileParser(ddfValidator);
            return ddfFileParser.parseEx(new ByteArrayInputStream(xmlByte), streamName).get(0);
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML file [{}]", streamName, e);
            return null;
        }
    }

    /**
     * @param value - info about Logs
     * @return- KeyValueProto for telemetry (Logs)
     */
    public List<TransportProtos.KeyValueProto> getKvStringtoThingsboard(String key, String value) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        value = value.replaceAll("<", "").replaceAll(">", "");
        result.add(TransportProtos.KeyValueProto.newBuilder()
                .setKey(key)
                .setType(TransportProtos.KeyValueType.STRING_V)
                .setStringV(value).build());
        return result;
    }

    /**
     * @return - KeyValueProto for attribute/telemetry (change value)
     * @throws CodecException -
     */

    public TransportProtos.KeyValueProto getKvAttrTelemetryToThingsboard(ResourceModel.Type resourceType, String resourceName, Object value, boolean isMultiInstances) {
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(resourceName);
        if (isMultiInstances) {
            kvProto.setType(TransportProtos.KeyValueType.JSON_V)
                    .setJsonV((String) value);
        } else {
            switch (resourceType) {
                case BOOLEAN:
                    kvProto.setType(BOOLEAN_V).setBoolV((Boolean) value).build();
                    break;
                case STRING:
                case TIME:
                case OPAQUE:
                case OBJLNK:
                    kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV((String) value);
                    break;
                case INTEGER:
                    kvProto.setType(TransportProtos.KeyValueType.LONG_V).setLongV((Long) value);
                    break;
                case FLOAT:
                    kvProto.setType(TransportProtos.KeyValueType.DOUBLE_V).setDoubleV((Double) value);
            }
        }
        return kvProto.build();
    }

    /**
     * @param currentType  -
     * @param resourcePath -
     * @return
     */
    public static ResourceModel.Type getResourceModelTypeEqualsKvProtoValueType(ResourceModel.Type currentType, String resourcePath) {
        switch (currentType) {
            case BOOLEAN:
                return ResourceModel.Type.BOOLEAN;
            case STRING:
            case TIME:
            case OPAQUE:
            case OBJLNK:
                return ResourceModel.Type.STRING;
            case INTEGER:
                return ResourceModel.Type.INTEGER;
            case FLOAT:
                return ResourceModel.Type.FLOAT;
            default:
        }
        throw new CodecException("Invalid ResourceModel_Type for resource %s, got %s", resourcePath, currentType);
    }

    public static ContentFormat convertResourceModelTypeToContentFormat(ResourceModel.Type type) {
        switch (type) {
            case BOOLEAN:
            case STRING:
            case TIME:
            case INTEGER:
            case FLOAT:
                return ContentFormat.TLV;
            case OPAQUE:
                return ContentFormat.OPAQUE;
            case OBJLNK:
                return ContentFormat.LINK;
            default:
        }
        throw new CodecException("Invalid ResourceModel_Type for %s ContentFormat.", type);
    }

    public static ContentFormat getContentFormatByResourceModelType(ResourceModel resourceModel, ContentFormat contentFormat) {
        return contentFormat.equals(ContentFormat.TLV) ? convertResourceModelTypeToContentFormat(resourceModel.type) :
                contentFormat;
    }

    public static Object getValueFromKvProto(TransportProtos.KeyValueProto kv) {
        switch (kv.getType()) {
            case BOOLEAN_V:
                return kv.getBoolV();
            case LONG_V:
                return kv.getLongV();
            case DOUBLE_V:
                return kv.getDoubleV();
            case STRING_V:
                return kv.getStringV();
            case JSON_V:
                return kv.getJsonV();
        }
        return null;
    }
}
