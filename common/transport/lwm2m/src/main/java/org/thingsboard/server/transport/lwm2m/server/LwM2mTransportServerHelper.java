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
package org.thingsboard.server.transport.lwm2m.server;

import com.google.gson.JsonParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.util.TbDDFFileParser;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.BOOLEAN_V;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mTransportServerHelper {

    private final LwM2mTransportContext context;

    public void sendParametersOnThingsboardAttribute(List<TransportProtos.KeyValueProto> result, SessionInfoProto sessionInfo) {
        PostAttributeMsg.Builder request = PostAttributeMsg.newBuilder();
        request.addAllKv(result);
        PostAttributeMsg postAttributeMsg = request.build();
        context.getTransportService().process(sessionInfo, postAttributeMsg, TransportServiceCallback.EMPTY);
    }

    public void sendParametersOnThingsboardTelemetry(List<TransportProtos.KeyValueProto> kvList, SessionInfoProto sessionInfo, @Nullable Map<String, AtomicLong> keyTsLatestMaps){
        sendParametersOnThingsboardTelemetry(kvList, sessionInfo, keyTsLatestMaps, null);
    }

    public void sendParametersOnThingsboardTelemetry(List<TransportProtos.KeyValueProto> kvList, SessionInfoProto sessionInfo, @Nullable Map<String, AtomicLong> keyTsLatestMap, @Nullable Instant ts) {
        TransportProtos.TsKvListProto tsKvList = toTsKvList(kvList, keyTsLatestMap, ts);

        PostTelemetryMsg postTelemetryMsg = PostTelemetryMsg.newBuilder()
                .addTsKvList(tsKvList)
                .build();

        context.getTransportService().process(sessionInfo, postTelemetryMsg, TransportServiceCallback.EMPTY);
    }

    TransportProtos.TsKvListProto toTsKvList(List<TransportProtos.KeyValueProto> kvList, Map<String, AtomicLong> keyTsLatestMap, @Nullable Instant ts) {
        return TransportProtos.TsKvListProto.newBuilder()
                .setTs(ts == null ? getTs(kvList, keyTsLatestMap) : ts.toEpochMilli())
                .addAllKv(kvList)
                .build();
    }

    long getTs(List<TransportProtos.KeyValueProto> kvList, Map<String, AtomicLong> keyTsLatestMap) {
        if (keyTsLatestMap == null || kvList == null || kvList.isEmpty()) {
            return getCurrentTimeMillis();
        }

        return getTsByKey(kvList.get(0).getKey(), keyTsLatestMap, getCurrentTimeMillis());
    }

    long getTsByKey(@Nonnull String key, @Nonnull Map<String, AtomicLong> keyTsLatestMap, final long tsNow) {
        AtomicLong tsLatestAtomic = keyTsLatestMap.putIfAbsent(key, new AtomicLong(tsNow));
        if (tsLatestAtomic == null) {
            return tsNow; // it is a first known timestamp for this key. return as the latest
        }

        return compareAndSwapOrIncrementTsAtomically(tsLatestAtomic, tsNow);
    }

    /**
     * This algorithm is sensitive to wall-clock time shift.
     * Once time have shifted *backward*, the latest ts never came back.
     * Ts latest will be incremented until current time overtake the latest ts.
     * In normal environment without race conditions method will return current ts (wall-clock)
     * */
    long compareAndSwapOrIncrementTsAtomically(AtomicLong tsLatestAtomic, final long tsNow) {
        long tsLatest;
        while ((tsLatest = tsLatestAtomic.get()) < tsNow) {
            if (tsLatestAtomic.compareAndSet(tsLatest, tsNow)) {
                return tsNow; //swap successful
            }
        }
        return tsLatestAtomic.incrementAndGet(); //return next ms
    }

    /**
     * For the test ability to mock system timer
     * */
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
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

    public ObjectModel parseFromXmlToObjectModel(byte[] xmlByte, String streamName) {
        try {
            TbDDFFileParser ddfFileParser = new TbDDFFileParser();
            return ddfFileParser.parse(new ByteArrayInputStream(xmlByte), streamName).get(0);
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
                try {
                    return JsonParser.parseString(kv.getJsonV());
                } catch (Exception e) {
                    return null;
                }
        }
        return null;
    }

}
