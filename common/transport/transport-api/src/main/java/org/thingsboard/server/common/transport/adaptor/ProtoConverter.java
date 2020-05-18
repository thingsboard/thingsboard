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
package org.thingsboard.server.common.transport.adaptor;

import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.gw.GatewayProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProtoConverter {

    public static final JsonParser parser = new JsonParser();

    public static TransportProtos.PostTelemetryMsg convertToTelemetryProto(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        GatewayProtos.TsKvListProto protoPayload = GatewayProtos.TsKvListProto.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto = getTransportTsKvListProto(TransportProtos.TsKvListProto.newBuilder(), protoPayload);
        postTelemetryMsgBuilder.addTsKvList(tsKvListProto);
        return postTelemetryMsgBuilder.build();
    }

    public static TransportProtos.PostTelemetryMsg convertToTelemetryArrayProto(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        GatewayProtos.TsKvListProtoArray tsKvListProtoArray = GatewayProtos.TsKvListProtoArray.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        List<GatewayProtos.TsKvListProto> tsKvListProtoList = tsKvListProtoArray.getTsKvList();
        if (!CollectionUtils.isEmpty(tsKvListProtoList)) {
            List<TransportProtos.TsKvListProto> transportTsKvListProtoArray = new ArrayList<>();
            tsKvListProtoList.forEach(tsKvListProto -> {
                TransportProtos.TsKvListProto.Builder tsKvListBuilder = TransportProtos.TsKvListProto.newBuilder();
                TransportProtos.TsKvListProto transportTsKvListProto = getTransportTsKvListProto(tsKvListBuilder, tsKvListProto);
                transportTsKvListProtoArray.add(transportTsKvListProto);
            });
            postTelemetryMsgBuilder.addAllTsKvList(transportTsKvListProtoArray);
            return postTelemetryMsgBuilder.build();
        } else {
            throw new IllegalArgumentException("TsKv List is empty!");
        }
    }

    public static TransportProtos.PostAttributeMsg convertToAttributesProto(byte[] bytes) throws IllegalArgumentException, InvalidProtocolBufferException {
        GatewayProtos.KvListProto proto = GatewayProtos.KvListProto.parseFrom(bytes);
        List<GatewayProtos.KeyValueProto> kvList = proto.getKvList();
        if (!CollectionUtils.isEmpty(kvList)) {
            List<TransportProtos.KeyValueProto> keyValueProtos = getKeyValueProtos(kvList);
            TransportProtos.PostAttributeMsg.Builder result = TransportProtos.PostAttributeMsg.newBuilder();
            result.addAllKv(keyValueProtos);
            return result.build();
        } else {
            throw new IllegalArgumentException("KeyValue List is empty!");
        }
    }

    public static TransportProtos.ClaimDeviceMsg convertToClaimDeviceProto(DeviceId deviceId, byte[] bytes) throws IllegalArgumentException, InvalidProtocolBufferException {
        GatewayProtos.ClaimDevice proto = GatewayProtos.ClaimDevice.parseFrom(bytes);
        String secretKey = proto.getSecretKey() != null ? proto.getSecretKey() : DataConstants.DEFAULT_SECRET_KEY;
        long durationMs = proto.getDurationMs();
        return buildClaimDeviceMsg(deviceId, secretKey, durationMs);
    }

    private static TransportProtos.ClaimDeviceMsg buildClaimDeviceMsg(DeviceId deviceId, String secretKey, long durationMs) {
        TransportProtos.ClaimDeviceMsg.Builder result = TransportProtos.ClaimDeviceMsg.newBuilder();
        return result
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setSecretKey(secretKey)
                .setDurationMs(durationMs)
                .build();
    }

    private static TransportProtos.TsKvListProto getTransportTsKvListProto(TransportProtos.TsKvListProto.Builder tsKvListBuilder, GatewayProtos.TsKvListProto protoPayload) {
        long ts = protoPayload.getTs();
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        tsKvListBuilder.setTs(ts);
        List<GatewayProtos.KeyValueProto> kvList = protoPayload.getKvList();
        if (!CollectionUtils.isEmpty(kvList)) {
            List<TransportProtos.KeyValueProto> keyValueListProtos = getKeyValueProtos(kvList);
            tsKvListBuilder.addAllKv(keyValueListProtos);
            return tsKvListBuilder.build();
        } else {
            throw new IllegalArgumentException("KeyValue List is empty!");
        }
    }

    private static List<TransportProtos.KeyValueProto> getKeyValueProtos(List<GatewayProtos.KeyValueProto> kvList) {
        List<TransportProtos.KeyValueProto> keyValueListProtos = new ArrayList<>();
        kvList.forEach(keyValueProto -> {
            String key = keyValueProto.getKey();
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Invalid key value: " + key + "!");
            }
            GatewayProtos.KeyValueType type = keyValueProto.getType();
            switch (type) {
                case BOOLEAN_V:
                    keyValueListProtos.add(TransportProtos.KeyValueProto.newBuilder()
                            .setKey(key)
                            .setType(TransportProtos.KeyValueType.BOOLEAN_V)
                            .setBoolV(keyValueProto.getBoolV())
                            .build());
                    break;
                case LONG_V:
                    keyValueListProtos.add(TransportProtos.KeyValueProto.newBuilder()
                            .setKey(key)
                            .setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(keyValueProto.getLongV())
                            .build());
                    break;
                case DOUBLE_V:
                    keyValueListProtos.add(TransportProtos.KeyValueProto.newBuilder()
                            .setKey(key)
                            .setType(TransportProtos.KeyValueType.DOUBLE_V)
                            .setDoubleV(keyValueProto.getLongV())
                            .build());
                    break;
                case STRING_V:
                    if (StringUtils.isEmpty(keyValueProto.getStringV())) {
                        throw new IllegalArgumentException("Value is empty for Key: " + key + "!");
                    }
                    keyValueListProtos.add(TransportProtos.KeyValueProto.newBuilder()
                            .setKey(key)
                            .setType(TransportProtos.KeyValueType.STRING_V)
                            .setStringV(keyValueProto.getStringV())
                            .build());
                    break;
                case JSON_V:
                    if (StringUtils.isEmpty(keyValueProto.getJsonV())) {
                        throw new IllegalArgumentException("Value is empty for Key: " + key + "!");
                    }
                    keyValueListProtos.add(TransportProtos.KeyValueProto.newBuilder()
                            .setKey(key)
                            .setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(keyValueProto.getJsonV())
                            .build());
                    break;
                case BYTES_V:
                    break;
                case UNRECOGNIZED:
                case UNKNOWN:
                    throw new IllegalArgumentException("Unsupported KeyValueType: " + type + "!");
            }
        });
        return keyValueListProtos;
    }
}
