/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.adaptor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.core.AttributesUpdateRequest;
import org.thingsboard.server.common.msg.core.BasicAttributesUpdateRequest;
import org.thingsboard.server.common.msg.core.BasicRequest;
import org.thingsboard.server.common.msg.core.BasicTelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.ToDeviceRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToServerRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToServerRpcResponseMsg;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonConverter {

    private static final Gson GSON = new Gson();
    public static final String CAN_T_PARSE_VALUE = "Can't parse value: ";

    public static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject) throws JsonSyntaxException {
        return convertToTelemetry(jsonObject, BasicRequest.DEFAULT_REQUEST_ID);
    }

    public static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject, long ts) throws JsonSyntaxException {
        return convertToTelemetry(jsonObject, ts, BasicRequest.DEFAULT_REQUEST_ID);
    }

    public static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject, int requestId) throws JsonSyntaxException {
        return convertToTelemetry(jsonObject, System.currentTimeMillis(), requestId);
    }

    public static PostTelemetryMsg convertToTelemetryProto(JsonElement jsonObject) throws JsonSyntaxException {
        long systemTs = System.currentTimeMillis();
        PostTelemetryMsg.Builder builder = PostTelemetryMsg.newBuilder();
        if (jsonObject.isJsonObject()) {
            parseObject(builder, systemTs, jsonObject);
        } else if (jsonObject.isJsonArray()) {
            jsonObject.getAsJsonArray().forEach(je -> {
                if (je.isJsonObject()) {
                    parseObject(builder, systemTs, je.getAsJsonObject());
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + je);
                }
            });
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonObject);
        }
        return builder.build();
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


    private static void parseObject(PostTelemetryMsg.Builder builder, long systemTs, JsonElement jsonObject) {
        JsonObject jo = jsonObject.getAsJsonObject();
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

    public static void parseWithTs(PostTelemetryMsg.Builder request, JsonObject jo) {
        TsKvListProto.Builder builder = TsKvListProto.newBuilder();
        builder.setTs(jo.get("ts").getAsLong());
        builder.addAllKv(parseProtoValues(jo.get("values").getAsJsonObject()));
        request.addTsKvList(builder.build());
    }

    public static List<KeyValueProto> parseProtoValues(JsonObject valuesObject) {
        List<KeyValueProto> result = new ArrayList<>();
        for (Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.STRING_V)
                            .setStringV(value.getAsString()).build());
                } else if (value.isBoolean()) {
                    result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.BOOLEAN_V)
                            .setBoolV(value.getAsBoolean()).build());
                } else if (value.isNumber()) {
                    if (value.getAsString().contains(".")) {
                        result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.DOUBLE_V)
                                .setDoubleV(value.getAsDouble()).build());
                    } else {
                        try {
                            long longValue = Long.parseLong(value.getAsString());
                            result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.LONG_V)
                                    .setLongV(longValue).build());
                        } catch (NumberFormatException e) {
                            throw new JsonSyntaxException("Big integer values are not supported!");
                        }
                    }
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
    }

    private static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject, long systemTs, int requestId) throws JsonSyntaxException {
        BasicTelemetryUploadRequest request = new BasicTelemetryUploadRequest(requestId);
        if (jsonObject.isJsonObject()) {
            parseObject(request, systemTs, jsonObject);
        } else if (jsonObject.isJsonArray()) {
            jsonObject.getAsJsonArray().forEach(je -> {
                if (je.isJsonObject()) {
                    parseObject(request, systemTs, je.getAsJsonObject());
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + je);
                }
            });
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonObject);
        }
        return request;
    }

    public static ToServerRpcRequestMsg convertToServerRpcRequest(JsonElement json, int requestId) throws JsonSyntaxException {
        JsonObject object = json.getAsJsonObject();
        return new ToServerRpcRequestMsg(requestId, object.get("method").getAsString(), GSON.toJson(object.get("params")));
    }

    private static void parseObject(BasicTelemetryUploadRequest request, long systemTs, JsonElement jsonObject) {
        JsonObject jo = jsonObject.getAsJsonObject();
        if (jo.has("ts") && jo.has("values")) {
            parseWithTs(request, jo);
        } else {
            parseWithoutTs(request, systemTs, jo);
        }
    }

    private static void parseWithoutTs(BasicTelemetryUploadRequest request, long systemTs, JsonObject jo) {
        for (KvEntry entry : parseValues(jo)) {
            request.add(systemTs, entry);
        }
    }

    public static void parseWithTs(BasicTelemetryUploadRequest request, JsonObject jo) {
        long ts = jo.get("ts").getAsLong();
        JsonObject valuesObject = jo.get("values").getAsJsonObject();
        for (KvEntry entry : parseValues(valuesObject)) {
            request.add(ts, entry);
        }
    }

    public static List<KvEntry> parseValues(JsonObject valuesObject) {
        List<KvEntry> result = new ArrayList<>();
        for (Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
                } else if (value.isBoolean()) {
                    result.add(new BooleanDataEntry(valueEntry.getKey(), value.getAsBoolean()));
                } else if (value.isNumber()) {
                    parseNumericValue(result, valueEntry, value);
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
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

    public static AttributesUpdateRequest convertToAttributes(JsonElement element) {
        return convertToAttributes(element, BasicRequest.DEFAULT_REQUEST_ID);
    }

    public static AttributesUpdateRequest convertToAttributes(JsonElement element, int requestId) {
        if (element.isJsonObject()) {
            BasicAttributesUpdateRequest request = new BasicAttributesUpdateRequest(requestId);
            long ts = System.currentTimeMillis();
            request.add(parseValues(element.getAsJsonObject()).stream().map(kv -> new BaseAttributeKvEntry(kv, ts)).collect(Collectors.toList()));
            return request;
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
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
        if (payload.getDeletedAttributeKeysCount() > 0) {
            JsonArray attrObject = new JsonArray();
            payload.getDeletedAttributeKeysList().forEach(attrObject::add);
            result.add("deleted", attrObject);
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

    public static JsonObject toJson(AttributesKVMsg payload, boolean asMap) {
        JsonObject result = new JsonObject();
        if (asMap) {
            if (!payload.getClientAttributes().isEmpty()) {
                JsonObject attrObject = new JsonObject();
                payload.getClientAttributes().forEach(addToObject(attrObject));
                result.add("client", attrObject);
            }
            if (!payload.getSharedAttributes().isEmpty()) {
                JsonObject attrObject = new JsonObject();
                payload.getSharedAttributes().forEach(addToObject(attrObject));
                result.add("shared", attrObject);
            }
        } else {
            payload.getClientAttributes().forEach(addToObject(result));
            payload.getSharedAttributes().forEach(addToObject(result));
        }
        if (!payload.getDeletedAttributes().isEmpty()) {
            JsonArray attrObject = new JsonArray();
            payload.getDeletedAttributes().forEach(addToObject(attrObject));
            result.add("deleted", attrObject);
        }
        return result;
    }

    private static Consumer<AttributeKey> addToObject(JsonArray result) {
        return key -> result.add(key.getAttributeKey());
    }

    private static Consumer<TsKvProto> addToObjectFromProto(JsonObject result) {
        return de -> {
            JsonPrimitive value;
            switch (de.getKv().getType()) {
                case BOOLEAN_V:
                    value = new JsonPrimitive(de.getKv().getBoolV());
                    break;
                case DOUBLE_V:
                    value = new JsonPrimitive(de.getKv().getDoubleV());
                    break;
                case LONG_V:
                    value = new JsonPrimitive(de.getKv().getLongV());
                    break;
                case STRING_V:
                    value = new JsonPrimitive(de.getKv().getStringV());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getKv().getType());
            }
            result.add(de.getKv().getKey(), value);
        };
    }

    private static Consumer<AttributeKvEntry> addToObject(JsonObject result) {
        return de -> {
            JsonPrimitive value;
            switch (de.getDataType()) {
                case BOOLEAN:
                    value = new JsonPrimitive(de.getBooleanValue().get());
                    break;
                case DOUBLE:
                    value = new JsonPrimitive(de.getDoubleValue().get());
                    break;
                case LONG:
                    value = new JsonPrimitive(de.getLongValue().get());
                    break;
                case STRING:
                    value = new JsonPrimitive(de.getStrValue().get());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getDataType());
            }
            result.add(de.getKey(), value);
        };
    }

    public static JsonObject toJson(ToDeviceRpcRequestMsg msg, boolean includeRequestId) {
        JsonObject result = new JsonObject();
        if (includeRequestId) {
            result.addProperty("id", msg.getRequestId());
        }
        result.addProperty("method", msg.getMethod());
        result.add("params", new JsonParser().parse(msg.getParams()));
        return result;
    }

    public static JsonElement toJson(ToServerRpcResponseMsg msg) {
        return new JsonParser().parse(msg.getData());
    }

    public static JsonElement toErrorJson(String errorMsg) {
        JsonObject error = new JsonObject();
        error.addProperty("error", errorMsg);
        return error;
    }

}
