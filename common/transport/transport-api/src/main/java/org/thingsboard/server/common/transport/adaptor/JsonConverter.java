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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsType;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvListProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateBasicMqttCredRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonConverter {

    private static final Gson GSON = new Gson();
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";

    private static boolean isTypeCastEnabled = true;

    private static int maxStringValueLength = 0;

    public static PostTelemetryMsg convertToTelemetryProto(JsonElement jsonElement) throws JsonSyntaxException {
        PostTelemetryMsg.Builder builder = PostTelemetryMsg.newBuilder();
        convertToTelemetry(jsonElement, System.currentTimeMillis(), null, builder);
        return builder.build();
    }

    private static void convertToTelemetry(JsonElement jsonElement, long systemTs, Map<Long, List<KvEntry>> result, PostTelemetryMsg.Builder builder) {
        if (jsonElement.isJsonObject()) {
            parseObject(systemTs, result, builder, jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            jsonElement.getAsJsonArray().forEach(je -> {
                if (je.isJsonObject()) {
                    parseObject(systemTs, result, builder, je.getAsJsonObject());
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + je);
                }
            });
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
    }

    private static void parseObject(long systemTs, Map<Long, List<KvEntry>> result, PostTelemetryMsg.Builder builder, JsonObject jo) {
        if (result != null) {
            parseObject(result, systemTs, jo);
        } else {
            parseObject(builder, systemTs, jo);
        }
    }

    public static ClaimDeviceMsg convertToClaimDeviceProto(DeviceId deviceId, String json) {
        long durationMs = 0L;
        if (json != null && !json.isEmpty()) {
            return convertToClaimDeviceProto(deviceId, new JsonParser().parse(json));
        }
        return buildClaimDeviceMsg(deviceId, DataConstants.DEFAULT_SECRET_KEY, durationMs);
    }

    public static ClaimDeviceMsg convertToClaimDeviceProto(DeviceId deviceId, JsonElement jsonElement) {
        String secretKey = DataConstants.DEFAULT_SECRET_KEY;
        long durationMs = 0L;
        if (jsonElement.isJsonObject()) {
            JsonObject jo = jsonElement.getAsJsonObject();
            if (jo.has(DataConstants.SECRET_KEY_FIELD_NAME)) {
                secretKey = jo.get(DataConstants.SECRET_KEY_FIELD_NAME).getAsString();
            }
            if (jo.has(DataConstants.DURATION_MS_FIELD_NAME)) {
                durationMs = jo.get(DataConstants.DURATION_MS_FIELD_NAME).getAsLong();
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
        return buildClaimDeviceMsg(deviceId, secretKey, durationMs);
    }

    private static ClaimDeviceMsg buildClaimDeviceMsg(DeviceId deviceId, String secretKey, long durationMs) {
        ClaimDeviceMsg.Builder result = ClaimDeviceMsg.newBuilder();
        return result
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setSecretKey(secretKey)
                .setDurationMs(durationMs)
                .build();
    }

    public static PostAttributeMsg convertToAttributesProto(JsonElement jsonObject) throws JsonSyntaxException {
        if (jsonObject.isJsonObject()) {
            PostAttributeMsg.Builder result = PostAttributeMsg.newBuilder();
            List<KeyValueProto> keyValueList = parseProtoValues(jsonObject.getAsJsonObject());
            result.addAllKv(keyValueList);
            return result.build();
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonObject);
        }
    }

    public static JsonElement toJson(TransportProtos.ToDeviceRpcRequestMsg msg, boolean includeRequestId) {
        JsonObject result = new JsonObject();
        if (includeRequestId) {
            result.addProperty("id", msg.getRequestId());
        }
        result.addProperty("method", msg.getMethodName());
        result.add("params", new JsonParser().parse(msg.getParams()));
        return result;
    }

    private static void parseObject(PostTelemetryMsg.Builder builder, long systemTs, JsonObject jo) {
        if (jo.has("ts") && jo.has("values")) {
            parseWithTs(builder, jo);
        } else {
            parseWithoutTs(builder, systemTs, jo);
        }
    }

    private static void parseWithoutTs(PostTelemetryMsg.Builder request, long systemTs, JsonObject jo) {
        TsKvListProto.Builder builder = TsKvListProto.newBuilder();
        builder.setTs(systemTs);
        builder.addAllKv(parseProtoValues(jo));
        request.addTsKvList(builder.build());
    }

    private static void parseWithTs(PostTelemetryMsg.Builder request, JsonObject jo) {
        TsKvListProto.Builder builder = TsKvListProto.newBuilder();
        builder.setTs(jo.get("ts").getAsLong());
        builder.addAllKv(parseProtoValues(jo.get("values").getAsJsonObject()));
        request.addTsKvList(builder.build());
    }

    private static List<KeyValueProto> parseProtoValues(JsonObject valuesObject) {
        List<KeyValueProto> result = new ArrayList<>();
        for (Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    if (maxStringValueLength > 0 && value.getAsString().length() > maxStringValueLength) {
                        String message = String.format("String value length [%d] for key [%s] is greater than maximum allowed [%d]", value.getAsString().length(), valueEntry.getKey(), maxStringValueLength);
                        throw new JsonSyntaxException(message);
                    }
                    if (isTypeCastEnabled && NumberUtils.isParsable(value.getAsString())) {
                        try {
                            result.add(buildNumericKeyValueProto(value, valueEntry.getKey()));
                        } catch (RuntimeException th) {
                            result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.STRING_V)
                                    .setStringV(value.getAsString()).build());
                        }
                    } else {
                        result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.STRING_V)
                                .setStringV(value.getAsString()).build());
                    }
                } else if (value.isBoolean()) {
                    result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.BOOLEAN_V)
                            .setBoolV(value.getAsBoolean()).build());
                } else if (value.isNumber()) {
                    result.add(buildNumericKeyValueProto(value, valueEntry.getKey()));
                } else if (!value.isJsonNull()) {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else if (element.isJsonObject() || element.isJsonArray()) {
                result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.JSON_V).setJsonV(element.toString()).build());
            } else if (!element.isJsonNull()) {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
    }

    private static KeyValueProto buildNumericKeyValueProto(JsonPrimitive value, String key) {
        if (value.getAsString().contains(".")) {
            return KeyValueProto.newBuilder()
                    .setKey(key)
                    .setType(KeyValueType.DOUBLE_V)
                    .setDoubleV(value.getAsDouble())
                    .build();
        } else {
            try {
                long longValue = Long.parseLong(value.getAsString());
                return KeyValueProto.newBuilder().setKey(key).setType(KeyValueType.LONG_V)
                        .setLongV(longValue).build();
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException("Big integer values are not supported!");
            }
        }
    }

    public static TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(JsonElement json, int requestId) throws JsonSyntaxException {
        JsonObject object = json.getAsJsonObject();
        return TransportProtos.ToServerRpcRequestMsg.newBuilder().setRequestId(requestId).setMethodName(object.get("method").getAsString()).setParams(GSON.toJson(object.get("params"))).build();
    }

    private static void parseNumericValue(List<KvEntry> result, Entry<String, JsonElement> valueEntry, JsonPrimitive value) {
        if (value.getAsString().contains(".")) {
            result.add(new DoubleDataEntry(valueEntry.getKey(), value.getAsDouble()));
        } else {
            try {
                long longValue = Long.parseLong(value.getAsString());
                result.add(new LongDataEntry(valueEntry.getKey(), longValue));
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException("Big integer values are not supported!");
            }
        }
    }

    public static JsonObject toJson(GetAttributeResponseMsg payload) {
        JsonObject result = new JsonObject();
        if (payload.getClientAttributeListCount() > 0) {
            JsonObject attrObject = new JsonObject();
            payload.getClientAttributeListList().forEach(addToObjectFromProto(attrObject));
            result.add("client", attrObject);
        }
        if (payload.getSharedAttributeListCount() > 0) {
            JsonObject attrObject = new JsonObject();
            payload.getSharedAttributeListList().forEach(addToObjectFromProto(attrObject));
            result.add("shared", attrObject);
        }
        return result;
    }

    public static JsonElement toJson(AttributeUpdateNotificationMsg payload) {
        JsonObject result = new JsonObject();
        if (payload.getSharedUpdatedCount() > 0) {
            payload.getSharedUpdatedList().forEach(addToObjectFromProto(result));
        }
        if (payload.getSharedDeletedCount() > 0) {
            JsonArray attrObject = new JsonArray();
            payload.getSharedDeletedList().forEach(attrObject::add);
            result.add("deleted", attrObject);
        }
        return result;
    }

    public static JsonObject getJsonObjectForGateway(String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) {
        JsonObject result = new JsonObject();
        result.addProperty("id", responseMsg.getRequestId());
        result.addProperty(DEVICE_PROPERTY, deviceName);
        if (responseMsg.getClientAttributeListCount() > 0) {
            addValues(result, responseMsg.getClientAttributeListList());
        }
        if (responseMsg.getSharedAttributeListCount() > 0) {
            addValues(result, responseMsg.getSharedAttributeListList());
        }
        return result;
    }

    public static JsonObject getJsonObjectForGateway(String deviceName, AttributeUpdateNotificationMsg notificationMsg) {
        JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", toJson(notificationMsg));
        return result;
    }

    private static void addValues(JsonObject result, List<TransportProtos.TsKvProto> kvList) {
        if (kvList.size() == 1) {
            addValueToJson(result, "value", kvList.get(0).getKv());
        } else {
            JsonObject values;
            if (result.has("values")) {
                values = result.get("values").getAsJsonObject();
            } else {
                values = new JsonObject();
                result.add("values", values);
            }
            kvList.forEach(value -> addValueToJson(values, value.getKv().getKey(), value.getKv()));
        }
    }

    private static void addValueToJson(JsonObject json, String name, TransportProtos.KeyValueProto entry) {
        switch (entry.getType()) {
            case BOOLEAN_V:
                json.addProperty(name, entry.getBoolV());
                break;
            case STRING_V:
                json.addProperty(name, entry.getStringV());
                break;
            case DOUBLE_V:
                json.addProperty(name, entry.getDoubleV());
                break;
            case LONG_V:
                json.addProperty(name, entry.getLongV());
                break;
            case JSON_V:
                json.add(name, JSON_PARSER.parse(entry.getJsonV()));
                break;
        }
    }

    private static Consumer<TsKvProto> addToObjectFromProto(JsonObject result) {
        return de -> {
            switch (de.getKv().getType()) {
                case BOOLEAN_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getBoolV()));
                    break;
                case DOUBLE_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getDoubleV()));
                    break;
                case LONG_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getLongV()));
                    break;
                case STRING_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getStringV()));
                    break;
                case JSON_V:
                    result.add(de.getKv().getKey(), JSON_PARSER.parse(de.getKv().getJsonV()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getKv().getType());
            }
        };
    }

    private static Consumer<AttributeKvEntry> addToObject(JsonObject result) {
        return de -> {
            switch (de.getDataType()) {
                case BOOLEAN:
                    result.add(de.getKey(), new JsonPrimitive(de.getBooleanValue().get()));
                    break;
                case DOUBLE:
                    result.add(de.getKey(), new JsonPrimitive(de.getDoubleValue().get()));
                    break;
                case LONG:
                    result.add(de.getKey(), new JsonPrimitive(de.getLongValue().get()));
                    break;
                case STRING:
                    result.add(de.getKey(), new JsonPrimitive(de.getStrValue().get()));
                    break;
                case JSON:
                    result.add(de.getKey(), JSON_PARSER.parse(de.getJsonValue().get()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getDataType());
            }
        };
    }

    public static JsonElement toJson(TransportProtos.ToServerRpcResponseMsg msg) {
        if (StringUtils.isEmpty(msg.getError())) {
            return new JsonParser().parse(msg.getPayload());
        } else {
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("error", msg.getError());
            return errorMsg;
        }
    }

    public static JsonObject toJson(ProvisionDeviceResponseMsg payload) {
        return toJson(payload, false, 0);
    }

    public static JsonObject toJson(ProvisionDeviceResponseMsg payload, int requestId) {
        return toJson(payload, true, requestId);
    }

    private static JsonObject toJson(ProvisionDeviceResponseMsg payload, boolean toGateway, int requestId) {
        JsonObject result = new JsonObject();
        if (payload.getStatus() == TransportProtos.ProvisionResponseStatus.NOT_FOUND) {
            result.addProperty("errorMsg", "Provision data was not found!");
            result.addProperty("status", ProvisionResponseStatus.NOT_FOUND.name());
        } else if (payload.getStatus() == TransportProtos.ProvisionResponseStatus.FAILURE) {
            result.addProperty("errorMsg", "Failed to provision device!");
            result.addProperty("status", ProvisionResponseStatus.FAILURE.name());
        } else {
            if (toGateway) {
                result.addProperty("id", requestId);
            }
            switch (payload.getCredentialsType()) {
                case ACCESS_TOKEN:
                case X509_CERTIFICATE:
                    result.addProperty("credentialsValue", payload.getCredentialsValue());
                    break;
                case MQTT_BASIC:
                    result.add("credentialsValue", JSON_PARSER.parse(payload.getCredentialsValue()).getAsJsonObject());
                    break;
                case LWM2M_CREDENTIALS:
                    break;
            }
            result.addProperty("credentialsType", payload.getCredentialsType().name());
            result.addProperty("status", ProvisionResponseStatus.SUCCESS.name());
        }
        return result;
    }

    public static JsonElement toErrorJson(String errorMsg) {
        JsonObject error = new JsonObject();
        error.addProperty("error", errorMsg);
        return error;
    }

    public static JsonElement toGatewayJson(String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", JsonConverter.toJson(rpcRequest, true));
        return result;
    }

    public static JsonElement toGatewayJson(String deviceName, TransportProtos.ProvisionDeviceResponseMsg responseRequest) {
        JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", JsonConverter.toJson(responseRequest));
        return result;
    }

    public static Set<AttributeKvEntry> convertToAttributes(JsonElement element) {
        Set<AttributeKvEntry> result = new HashSet<>();
        long ts = System.currentTimeMillis();
        result.addAll(parseValues(element.getAsJsonObject()).stream().map(kv -> new BaseAttributeKvEntry(kv, ts)).collect(Collectors.toList()));
        return result;
    }

    private static List<KvEntry> parseValues(JsonObject valuesObject) {
        List<KvEntry> result = new ArrayList<>();
        for (Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    if (maxStringValueLength > 0 && value.getAsString().length() > maxStringValueLength) {
                        String message = String.format("String value length [%d] for key [%s] is greater than maximum allowed [%d]", value.getAsString().length(), valueEntry.getKey(), maxStringValueLength);
                        throw new JsonSyntaxException(message);
                    }
                    if (isTypeCastEnabled && NumberUtils.isParsable(value.getAsString())) {
                        try {
                            parseNumericValue(result, valueEntry, value);
                        } catch (RuntimeException th) {
                            result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
                        }
                    } else {
                        result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
                    }
                } else if (value.isBoolean()) {
                    result.add(new BooleanDataEntry(valueEntry.getKey(), value.getAsBoolean()));
                } else if (value.isNumber()) {
                    parseNumericValue(result, valueEntry, value);
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else if (element.isJsonObject() || element.isJsonArray()) {
                result.add(new JsonDataEntry(valueEntry.getKey(), element.toString()));
            } else {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
    }

    public static Map<Long, List<KvEntry>> convertToTelemetry(JsonElement jsonElement, long systemTs) throws JsonSyntaxException {
        return convertToTelemetry(jsonElement, systemTs, false);
    }

    public static Map<Long, List<KvEntry>> convertToSortedTelemetry(JsonElement jsonElement, long systemTs) throws JsonSyntaxException {
        return convertToTelemetry(jsonElement, systemTs, true);
    }

    public static Map<Long, List<KvEntry>> convertToTelemetry(JsonElement jsonElement, long systemTs, boolean sorted) throws JsonSyntaxException {
        Map<Long, List<KvEntry>> result = sorted ? new TreeMap<>() : new HashMap<>();
        convertToTelemetry(jsonElement, systemTs, result, null);
        return result;
    }


    private static void parseObject(Map<Long, List<KvEntry>> result, long systemTs, JsonObject jo) {
        if (jo.has("ts") && jo.has("values")) {
            parseWithTs(result, jo);
        } else {
            parseWithoutTs(result, systemTs, jo);
        }
    }

    private static void parseWithoutTs(Map<Long, List<KvEntry>> result, long systemTs, JsonObject jo) {
        for (KvEntry entry : parseValues(jo)) {
            result.computeIfAbsent(systemTs, tmp -> new ArrayList<>()).add(entry);
        }
    }

    public static void parseWithTs(Map<Long, List<KvEntry>> result, JsonObject jo) {
        long ts = jo.get("ts").getAsLong();
        JsonObject valuesObject = jo.get("values").getAsJsonObject();
        for (KvEntry entry : parseValues(valuesObject)) {
            result.computeIfAbsent(ts, tmp -> new ArrayList<>()).add(entry);
        }
    }

    public static void setTypeCastEnabled(boolean enabled) {
        isTypeCastEnabled = enabled;
    }

    public static void setMaxStringValueLength(int length) {
        maxStringValueLength = length;
    }

    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(String json) {
        JsonElement jsonElement = new JsonParser().parse(json);
        if (jsonElement.isJsonObject()) {
            return buildProvisionRequestMsg(jsonElement.getAsJsonObject());
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
    }

    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(JsonObject jo) {
        return buildProvisionRequestMsg(jo);
    }

    private static TransportProtos.ProvisionDeviceRequestMsg buildProvisionRequestMsg(JsonObject jo) {
        return TransportProtos.ProvisionDeviceRequestMsg.newBuilder()
                .setDeviceName(getStrValue(jo, DataConstants.DEVICE_NAME, false))
                .setCredentialsType(jo.get(DataConstants.CREDENTIALS_TYPE) != null ? TransportProtos.CredentialsType.valueOf(getStrValue(jo, DataConstants.CREDENTIALS_TYPE, false)) : CredentialsType.ACCESS_TOKEN)
                .setCredentialsDataProto(TransportProtos.CredentialsDataProto.newBuilder()
                        .setValidateDeviceTokenRequestMsg(ValidateDeviceTokenRequestMsg.newBuilder().setToken(getStrValue(jo, DataConstants.TOKEN, false)).build())
                        .setValidateBasicMqttCredRequestMsg(ValidateBasicMqttCredRequestMsg.newBuilder()
                                .setClientId(getStrValue(jo, DataConstants.CLIENT_ID, false))
                                .setUserName(getStrValue(jo, DataConstants.USERNAME, false))
                                .setPassword(getStrValue(jo, DataConstants.PASSWORD, false))
                                .build())
                        .setValidateDeviceX509CertRequestMsg(ValidateDeviceX509CertRequestMsg.newBuilder()
                                .setHash(getStrValue(jo, DataConstants.HASH, false)).build())
                        .build())
                .setProvisionDeviceCredentialsMsg(buildProvisionDeviceCredentialsMsg(
                        getStrValue(jo, DataConstants.PROVISION_KEY, true),
                        getStrValue(jo, DataConstants.PROVISION_SECRET, true)))
                .build();
    }

    private static TransportProtos.ProvisionDeviceCredentialsMsg buildProvisionDeviceCredentialsMsg(String provisionKey, String provisionSecret) {
        return TransportProtos.ProvisionDeviceCredentialsMsg.newBuilder()
                .setProvisionDeviceKey(provisionKey)
                .setProvisionDeviceSecret(provisionSecret)
                .build();
    }


    private static String getStrValue(JsonObject jo, String field, boolean requiredField) {
        if (jo.has(field)) {
            return jo.get(field).getAsString();
        } else {
            if (requiredField) {
                throw new RuntimeException("Failed to find the field " + field + " in JSON body " + jo + "!");
            }
            return "";
        }
    }
}
