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
package org.thingsboard.server.transport.lwm2m.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.AttrTelemetryObserveValue;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component("LwM2MTransportHandler")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' )|| ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportHandler{

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    public static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms
    public static final String EVENT_DEREGISTRATION = "DEREGISTRATION";
    public static final String OBSERVE_ATTRIBUTE_TELEMETRY = "observeAttr";
    public static final String KEYNAME = "keyName";
    public static final String ATTRIBUTE = "attribute";
    public static final String TELEMETRY = "telemetry";
    public static final String OBSERVE = "observe";
    public static final String BASE_DEVICE_API_TOPIC = "v1/devices/me";
    public static final String DEVICE_ATTRIBUTES_TOPIC = BASE_DEVICE_API_TOPIC + "/attributes";
    public static final String DEVICE_TELEMETRY_TOPIC = BASE_DEVICE_API_TOPIC + "/telemetry";

    public static final String GET_TYPE_OPER_READ = "read";
    public static final String GET_TYPE_OPER_DISCOVER = "discover";
    public static final String GET_TYPE_OPER_OBSERVE = "observe";
    public static final String POST_TYPE_OPER_OBSERVE_CANCEL = "observeCancel";
    public static final String POST_TYPE_OPER_EXECUTE = "execute";
    public static final String PUT_TYPE_OPER_UPDATE = "update";
    public static final String PUT_TYPE_OPER_WRITE = "write";
    public static final String PUT_TYPE_OPER_WRITE_ATTRIBUTES = "wright-attributes";

    public static final String EVENT_AWAKE = "AWAKE";

    @Autowired
    @Qualifier("LeshanServerCert")
    private LeshanServer lhServerCert;

    @Autowired
    @Qualifier("leshanServerNoSecPskRpk")
    private LeshanServer lhServerNoSecPskRpk;

    @Autowired
    private LwM2MTransportService service;


    @PostConstruct
    public void init() {
        LwM2mServerListener lwM2mServerListener = new LwM2mServerListener(lhServerCert, service);
        this.lhServerCert.getRegistrationService().addListener(lwM2mServerListener.registrationListener);
        this.lhServerCert.getPresenceService().addListener(lwM2mServerListener.presenceListener);
        this.lhServerCert.getObservationService().addListener(lwM2mServerListener.observationListener);
        lwM2mServerListener = new LwM2mServerListener(lhServerNoSecPskRpk, service);
        this.lhServerNoSecPskRpk.getRegistrationService().addListener(lwM2mServerListener.registrationListener);
        this.lhServerNoSecPskRpk.getPresenceService().addListener(lwM2mServerListener.presenceListener);
        this.lhServerNoSecPskRpk.getObservationService().addListener(lwM2mServerListener.observationListener);
    }

    public static NetworkConfig getCoapConfig() {
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        return coapConfig;
    }

    public static String getValueTypeToString (Object value, ResourceModel.Type type) {
        switch (type) {
            case STRING:    // String
            case OBJLNK:    // ObjectLink
                return value.toString();
            case INTEGER:   // Long
                return Long.toString((long) value);
            case BOOLEAN:   // Boolean
                return Boolean.toString((Boolean) value);
            case FLOAT:     // Double
                return Double.toString((Float)value);
            case TIME:      // Date
                String DATE_FORMAT = "MMM d, yyyy HH:mm a";
                DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                return formatter.format(new Date((Long) Integer.toUnsignedLong(Integer.valueOf((Integer) value))));
            case OPAQUE:    // byte[] value, base64
                return Hex.encodeHexString((byte[])value);
            default:
                return null;
        }
    }

    public static LwM2mNode getLvM2mNodeToObject(LwM2mNode content) {
        if (content instanceof LwM2mObject) {
            return (LwM2mObject) content;
        } else if (content instanceof LwM2mObjectInstance) {
            return (LwM2mObjectInstance) content;
        } else if (content instanceof LwM2mSingleResource) {
            return (LwM2mSingleResource) content;
        } else if (content instanceof LwM2mMultipleResource) {
            return (LwM2mMultipleResource) content;
        }
        return null;
    }

    public static AttrTelemetryObserveValue getNewProfileParameters(JsonObject profilesConfigData) {
        AttrTelemetryObserveValue attrTelemetryObserveValue = new AttrTelemetryObserveValue();
        attrTelemetryObserveValue.setPostKeyNameProfile(profilesConfigData.get(KEYNAME).getAsJsonObject());
        attrTelemetryObserveValue.setPostAttributeProfile(profilesConfigData.get(ATTRIBUTE).getAsJsonArray());
        attrTelemetryObserveValue.setPostTelemetryProfile(profilesConfigData.get(TELEMETRY).getAsJsonArray());
        attrTelemetryObserveValue.setPostObserveProfile(profilesConfigData.get(OBSERVE).getAsJsonArray());
        return  attrTelemetryObserveValue;
    }

    /**

     * @return deviceProfileBody with Observe&Attribute&Telemetry From Thingsboard
     * Example: with pathResource (use only pathResource)
     * property: "observeAttr"
     * {"keyName": {
     *       "/3/0/1": "modelNumber",
     *       "/3/0/0": "manufacturer",
     *       "/3/0/2": "serialNumber"
     *       },
     * "attribute":["/2/0/1","/3/0/9"],
     * "telemetry":["/1/0/1","/2/0/1","/6/0/1"],
     * "observe":["/2/0","/2/0/0","/4/0/2"]}
     */
    public static JsonObject getObserveAttrTelemetryFromThingsboard(DeviceProfile deviceProfile) {
        if (deviceProfile != null && ((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties().size() > 0) {
            Lwm2mDeviceProfileTransportConfiguration lwm2mDeviceProfileTransportConfiguration = (Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            Object observeAttr = ((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties();
            try {
                ObjectMapper mapper = new ObjectMapper();
                String observeAttrStr = mapper.writeValueAsString(observeAttr);
                JsonObject objectMsg = (observeAttrStr != null) ? validateJson(observeAttrStr) : null;
                return (getValidateCredentialsBodyFromThingsboard(objectMsg)) ? objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject() : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static boolean getValidateCredentialsBodyFromThingsboard(JsonObject objectMsg) {
        return (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has(OBSERVE_ATTRIBUTE_TELEMETRY) &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonObject() &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(KEYNAME) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(KEYNAME).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(ATTRIBUTE) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(TELEMETRY) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(OBSERVE) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE).isJsonArray());
    }


    public static JsonObject validateJson(String jsonStr) {
        JsonObject object = null;
        if (jsonStr != null && !jsonStr.isEmpty()) {
            String jsonValidFlesh = jsonStr.replaceAll("\\\\", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\n", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\t", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll(" ", "");
            String jsonValid = (jsonValidFlesh.substring(0, 1).equals("\"") && jsonValidFlesh.substring(jsonValidFlesh.length() - 1).equals("\"")) ? jsonValidFlesh.substring(1, jsonValidFlesh.length() - 1) : jsonValidFlesh;
            try {
                object = new JsonParser().parse(jsonValid).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("[{}] Fail validateJson [{}]", jsonStr, e.getMessage());
            }
        }
        return object;
    }

    public static <T> Optional<T> decode(byte[] byteArray) {
        try {
            FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();;
            T msg = (T) config.asObject(byteArray);
            return Optional.ofNullable(msg);
        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
            return Optional.empty();
        }
    }

    public static UUID toSessionId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    /**
     * Equals to Map for values
     * @param map1
     * @param map2
     * @param <V>
     * @return
     */
    public static <V extends Comparable<V>>  boolean mapsEquals(Map<?,V> map1, Map<?,V> map2) {
        List<V> values1 = new ArrayList<V>(map1.values());
        List<V> values2 = new ArrayList<V>(map2.values());
        Collections.sort(values1);
        Collections.sort(values2);
        return values1.equals(values2);
    }
}
