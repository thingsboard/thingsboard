/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.JsonObject;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LwM2MClientSerDes {
    public static final String VALUE = "value";

    @SneakyThrows
    public static byte[] serialize(LwM2mClient client) {
        JsonObject o =  new JsonObject();
        o.addProperty("nodeId", "client.getNodeId()");
        o.addProperty("endpoint", client.getEndpoint());

        JsonObject resources = new JsonObject();
        client.getResources().forEach((k, v) -> {
            JsonObject resourceValue = new JsonObject();
            resourceValue.add("lwM2mResource", serialize(v.getLwM2mResource()));
            resourceValue.add("resourceModel", serialize(v.getResourceModel()));
            resources.add(k, resourceValue);
        });
        o.add("resources", resources);
        JsonObject sharedAttributes = new JsonObject();

        for (Map.Entry<String, TransportProtos.TsKvProto> entry : client.getSharedAttributes().entrySet()) {
            sharedAttributes.addProperty(entry.getKey(), JsonFormat.printer().print(entry.getValue()));
        }

        o.add("sharedAttributes", sharedAttributes);
        JsonObject keyTsLatestMap = new JsonObject();
        client.getKeyTsLatestMap().forEach((k, v) -> {
            keyTsLatestMap.addProperty(k, v.get());
        });
        o.add("keyTsLatestMap", keyTsLatestMap);

        o.addProperty("state", client.getState().toString());

        if (client.getSession() != null) {
            o.addProperty("session", JsonFormat.printer().print(client.getSession()));
        }
        if (client.getTenantId() != null) {
            o.addProperty("tenantId", client.getTenantId().toString());
        }
        if (client.getDeviceId() != null) {
            o.addProperty("deviceId", client.getDeviceId().toString());
        }
        if (client.getProfileId() != null) {
            o.addProperty("profileId", client.getProfileId().toString());
        }
        if (client.getPowerMode() != null) {
            o.addProperty("powerMode", client.getPowerMode().toString());
        }
        if (client.getEdrxCycle() != null) {
            o.addProperty("edrxCycle", client.getEdrxCycle());
        }
        if (client.getPsmActivityTimer() != null) {
            o.addProperty("psmActivityTimer", client.getPsmActivityTimer());
        }
        if (client.getPagingTransmissionWindow() != null) {
            o.addProperty("pagingTransmissionWindow", client.getPagingTransmissionWindow());
        }
        if (client.getRegistration() != null) {
            RegistrationSerDes regDez = new RegistrationSerDes();
            o.addProperty("registration", regDez.jSerialize(client.getRegistration()).toString());
        }
        o.addProperty("asleep", client.isAsleep());
        o.addProperty("lastUplinkTime", client.getLastUplinkTime());

        Field firstEdrxDownlink = LwM2mClient.class.getDeclaredField("firstEdrxDownlink");
        firstEdrxDownlink.setAccessible(true);
        o.addProperty("firstEdrxDownlink", (boolean) firstEdrxDownlink.get(client));
        o.addProperty("retryAttempts", client.getRetryAttempts().get());

        if (client.getLastSentRpcId() != null) {
            o.addProperty("lastSentRpcId", client.getLastSentRpcId().toString());
        }

        return o.toString().getBytes();
    }

    private static JsonObject serialize(LwM2mResource resource) {
        JsonObject o = new JsonObject();
        o.addProperty("id", resource.getId());
        o.addProperty("type", resource.getType().toString());
        if (resource.isMultiInstances()) {
            o.addProperty("multiInstances", true);
            JsonObject instances = new JsonObject();
            resource.getInstances().forEach((id, in) -> {
                JsonObject instance = new JsonObject();
                instance.addProperty("id", in.getId());
                addValue(instance, in.getType(), in.getValue());
                instances.add(id.toString(), instance);
            });
            o.add("instances", instances);
        } else {
            o.addProperty("multiInstances", false);
            addValue(o, resource.getType(), resource.getValue());
        }

        return o;
    }

    private static LwM2mResource parseLwM2mResource(JsonObject o) {
        boolean multiInstances = o.get("multiInstances").getAsBoolean();
        int id = o.get("id").getAsInt();
        ResourceModel.Type type = ResourceModel.Type.valueOf(o.get("type").getAsString());
        if (multiInstances) {
            Map<Integer, Object> instances = new HashMap<>();
            o.get("instances").getAsJsonArray().forEach(entry -> {
//                instances.put(Integer.valueOf(entry.getAsJsonObject().), parseValue(type, entry.getValue()));
            });
            return LwM2mMultipleResource.newResource(id, instances, type);
        } else {
            return LwM2mSingleResource.newResource(id, parseValue(type, (JsonValue) o.get(VALUE)));
        }
    }

    private static Object parseValue(ResourceModel.Type type, JsonValue value) {
        switch (type) {
            case INTEGER:
                return value.value();
//            case FLOAT:
//                return value.value();
//            case BOOLEAN:
//                return value.asBoolean();
//            case OPAQUE:
//                return Base64.getDecoder().decode(value.asString());
//            case STRING:
//                return value.asString();
//            case TIME:
//                return new Date(value.asLong());
//            case OBJLNK:
//                return ObjectLink.decodeFromString(value.asString());
//            case UNSIGNED_INTEGER:
//                return ULong.valueOf(value.asString());
            default:
                throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
    }

    private static JsonObject serialize(ResourceModel resourceModel) {
        JsonObject o = new JsonObject();
        o.addProperty("id", resourceModel.id);
        o.addProperty("name", resourceModel.name);
        o.addProperty("operations", resourceModel.operations.toString());
        o.addProperty("multiple", resourceModel.multiple);
        o.addProperty("mandatory", resourceModel.mandatory);
        o.addProperty("type", resourceModel.type.toString());
        o.addProperty("rangeEnumeration", resourceModel.rangeEnumeration);
        o.addProperty("units", resourceModel.units);
        o.addProperty("description", resourceModel.description);
        return o;
    }

    private static ResourceModel parseResourceModel(JsonObject o) {
        Integer id = o.get("id").getAsInt();
        String name = o.get("name").getAsString();
        ResourceModel.Operations operations = ResourceModel.Operations.valueOf(o.get("operations").getAsString());
        Boolean multiple = o.get("multiple").getAsBoolean();
        Boolean mandatory = o.get("mandatory").getAsBoolean();
        ResourceModel.Type type = ResourceModel.Type.valueOf(o.get("type").getAsString());
        String rangeEnumeration = o.get("rangeEnumeration").getAsString();
        String units = o.get("units").getAsString();
        String description = o.get("description").getAsString();
        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }

    private static void addValue(JsonObject o, ResourceModel.Type type, Object value) {
        switch (type) {
            case INTEGER:
                o.addProperty(VALUE, (Long) value);
                break;
            case FLOAT:
                o.addProperty(VALUE, (Double) value);
                break;
            case BOOLEAN:
                o.addProperty(VALUE, (Boolean) value);
                break;
            case OPAQUE:
                o.addProperty(VALUE, Base64.getEncoder().encodeToString((byte[]) value));
                break;
            case STRING:
                o.addProperty(VALUE, (String) value);
                break;
            case TIME:
                o.addProperty(VALUE, ((Date) value).getTime());
                break;
            case OBJLNK:
                o.addProperty(VALUE, ((ObjectLink) value).encodeToString());
                break;
            case UNSIGNED_INTEGER:
                o.addProperty(VALUE, value.toString());
                break;
            default:
                throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
    }

    @SneakyThrows
    public static LwM2mClient deserialize(byte[] data) {
//        JsonObject o = new JsonObject(new String(data)));
//        LwM2mClient lwM2mClient = new LwM2mClient(o.get("nodeId").getAsString(), o.get("endpoint").getAsString());
        LwM2mClient lwM2mClient = new LwM2mClient("nodeId", "endpoint");
//        o.get("resources").getAsJsonObject().forEach(entry -> {
//            JsonObject resource = entry.getValue().asObject();
//            LwM2mResource lwM2mResource = parseLwM2mResource(resource.get("lwM2mResource").getAsJsonObject());
//            ResourceModel resourceModel = parseResourceModel(resource.get("resourceModel").asObject());
//            ResourceValue resourceValue = new ResourceValue(lwM2mResource, resourceModel);
//            lwM2mClient.getResources().put(entry.getName(), resourceValue);
//        });
//
//        for (JsonObject.Member entry : o.get("sharedAttributes").asObject()) {
//            TransportProtos.TsKvProto.Builder builder = TransportProtos.TsKvProto.newBuilder();
//            JsonFormat.parser().merge(entry.getValue().getAsString(), builder);
//            lwM2mClient.getSharedAttributes().put(entry.getName(), builder.build());
//        }
//
//        o.get("keyTsLatestMap").asObject().forEach(entry -> {
//            lwM2mClient.getKeyTsLatestMap().put(entry.getName(), new AtomicLong(entry.getValue().asLong()));
//        });
//
//        lwM2mClient.setState(LwM2MClientState.valueOf(o.get("state").getAsString()));
//
//        Class<LwM2mClient> lwM2mClientClass = LwM2mClient.class;
//
//        JsonValue session = o.get("session");
//        if (session != null) {
//            TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder();
//            JsonFormat.parser().merge(session.asString(), builder);
//
//            Field sessionField = lwM2mClientClass.getDeclaredField("session");
//            sessionField.setAccessible(true);
//            sessionField.set(lwM2mClient, builder.build());
//        }
//
//        JsonValue tenantId = o.get("tenantId");
//        if (tenantId != null) {
//            Field tenantIdField = lwM2mClientClass.getDeclaredField("tenantId");
//            tenantIdField.setAccessible(true);
//            tenantIdField.set(lwM2mClient, new TenantId(UUID.fromString(tenantId.asString())));
//        }
//
//        JsonValue deviceId = o.get("deviceId");
//        if (tenantId != null) {
//            Field deviceIdField = lwM2mClientClass.getDeclaredField("deviceId");
//            deviceIdField.setAccessible(true);
//            deviceIdField.set(lwM2mClient, UUID.fromString(deviceId.asString()));
//        }
//
//        JsonValue profileId = o.get("profileId");
//        if (tenantId != null) {
//            Field profileIdField = lwM2mClientClass.getDeclaredField("profileId");
//            profileIdField.setAccessible(true);
//            profileIdField.set(lwM2mClient, UUID.fromString(profileId.asString()));
//        }
//
//        JsonValue powerMode = o.get("powerMode");
//        if (powerMode != null) {
//            Field powerModeField = lwM2mClientClass.getDeclaredField("powerMode");
//            powerModeField.setAccessible(true);
//            powerModeField.set(lwM2mClient, PowerMode.valueOf(powerMode.asString()));
//        }
//
//        JsonValue edrxCycle = o.get("edrxCycle");
//        if (edrxCycle != null) {
//            Field edrxCycleField = lwM2mClientClass.getDeclaredField("edrxCycle");
//            edrxCycleField.setAccessible(true);
//            edrxCycleField.set(lwM2mClient, edrxCycle.asLong());
//        }
//
//        JsonValue psmActivityTimer = o.get("psmActivityTimer");
//        if (psmActivityTimer != null) {
//            Field psmActivityTimerField = lwM2mClientClass.getDeclaredField("psmActivityTimer");
//            psmActivityTimerField.setAccessible(true);
//            psmActivityTimerField.set(lwM2mClient, psmActivityTimer.asLong());
//        }
//
//        JsonValue pagingTransmissionWindow = o.get("pagingTransmissionWindow");
//        if (pagingTransmissionWindow != null) {
//            Field pagingTransmissionWindowField = lwM2mClientClass.getDeclaredField("pagingTransmissionWindow");
//            pagingTransmissionWindowField.setAccessible(true);
//            pagingTransmissionWindowField.set(lwM2mClient, pagingTransmissionWindow.asLong());
//        }
//
//        JsonValue registration = o.get("registration");
//        if (registration != null) {
//            lwM2mClient.setRegistration(RegistrationSerDes.deserialize(registration.asObject()));
//        }
//
//        lwM2mClient.setAsleep(o.get("asleep").getAsBoolean());
//
//        Field lastUplinkTimeField = lwM2mClientClass.getDeclaredField("lastUplinkTime");
//        lastUplinkTimeField.setAccessible(true);
//        lastUplinkTimeField.setLong(lwM2mClient, o.get("lastUplinkTime").asLong());
//
//        Field firstEdrxDownlinkField = lwM2mClientClass.getDeclaredField("firstEdrxDownlink");
//        firstEdrxDownlinkField.setAccessible(true);
//        firstEdrxDownlinkField.setBoolean(lwM2mClient, o.get("firstEdrxDownlink").getAsBoolean());
//
//        lwM2mClient.getRetryAttempts().set(o.get("retryAttempts").asInt());
//
//        JsonValue lastSentRpcId = o.get("lastSentRpcId");
//        if (lastSentRpcId != null) {
//            lwM2mClient.setLastSentRpcId(UUID.fromString(lastSentRpcId.asString()));
//        }

        return lwM2mClient;
    }

}
