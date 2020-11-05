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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
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
    public static final String ATTRIBUTE = "attribute";
    public static final String TELEMETRY = "telemetry";
    public static final String OBSERVE = "observe";
    /**
     * The default key store FolderPath for reading Certificates from resource
     */
    public static final String KEY_STORE_DEFAULT_RESOURCE_PATH = "credentials/serverKeyStore.jks";

    /**
     * The default modelFolderPath for reading ObjectModel from resource
     */
    public static final String MODEL_DEFAULT_RESOURCE_PATH = "/models";
    /**
     * This field use to server.modelPaths
     * This field use to ClientDemo.modelPaths
     */
    public final static String[] modelPaths = {"10241.xml", "10242.xml", "10243.xml", "10244.xml",
            "10245.xml", "10246.xml", "10247.xml", "10248.xml", "10249.xml", "10250.xml", "10251.xml",
            "10252.xml", "10253.xml", "10254.xml", "10255.xml", "10256.xml", "10257.xml", "10258.xml",
            "10259.xml", "10260-2_0.xml", "10262.xml", "10263.xml", "10264.xml", "10265.xml",
            "10266.xml", "10267.xml", "10268.xml", "10269.xml", "10270.xml", "10271.xml", "10272.xml",
            "10273.xml", "10274.xml", "10275.xml", "10276.xml", "10277.xml", "10278.xml", "10279.xml",
            "10280.xml", "10281.xml", "10282.xml", "10283.xml", "10284.xml", "10286.xml", "10290.xml",
            "10291.xml", "10292.xml", "10299.xml", "10300.xml", "10308-2_0.xml", "10309.xml",
            "10311.xml", "10313.xml", "10314.xml", "10315.xml", "10316.xml", "10318.xml", "10319.xml",
            "10320.xml", "10322.xml", "10323.xml", "10324.xml", "10326.xml", "10327.xml", "10328.xml",
            "10329.xml", "10330.xml", "10331.xml", "10332.xml", "10333.xml", "10334.xml", "10335.xml",
            "10336.xml", "10337.xml", "10338.xml", "10339.xml", "10340.xml", "10341.xml", "10342.xml",
            "10343.xml", "10344.xml", "10345.xml", "10346.xml", "10347.xml", "10348.xml", "10349.xml",
            "10350.xml", "10351.xml", "10352.xml", "10353.xml", "10354.xml", "10355.xml", "10356.xml",
            "10357.xml", "10358.xml", "10359.xml", "10360.xml", "10361.xml", "10362.xml", "10363.xml",
            "10364.xml", "10365.xml", "10366.xml", "10368.xml", "10369.xml",

            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
            "2055.xml", "2056.xml", "2057.xml",

            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
            "3347.xml", "3348.xml", "3349.xml", "3350.xml", "3351.xml", "3352.xml", "3353.xml",
            "3354.xml", "3355.xml", "3356.xml", "3357.xml", "3358.xml", "3359.xml", "3360.xml",
            "3361.xml", "3362.xml", "3363.xml", "3364.xml", "3365.xml", "3366.xml", "3367.xml",
            "3368.xml", "3369.xml", "3370.xml", "3371.xml", "3372.xml", "3373.xml", "3374.xml",
            "3375.xml", "3376.xml", "3377.xml", "3378.xml", "3379.xml", "3380-2_0.xml", "3381.xml",
            "3382.xml", "3383.xml", "3384.xml", "3385.xml", "3386.xml",

            "LWM2M_APN_Connection_Profile-v1_0_1.xml", "LWM2M_Bearer_Selection-v1_0_1.xml",
            "LWM2M_Cellular_Connectivity-v1_0_1.xml", "LWM2M_DevCapMgmt-v1_0.xml",
            "LWM2M_LOCKWIPE-v1_0_1.xml", "LWM2M_Portfolio-v1_0.xml",
            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",
            "LWM2M_WLAN_connectivity4-v1_0.xml", "LwM2M_BinaryAppDataContainer-v1_0_1.xml",
            "LwM2M_EventLog-V1_0.xml", "LWM2M_Connectivity_Monitoring-v1_0_2.xml"};

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

//    public static KeyStore getInKeyStore(LwM2MTransportContextServer context) {
//        KeyStore keyStoreServer = null;
//        try {
//            if (context.getKeyStoreValue() != null && context.getKeyStoreValue().size() > 0)
//                return context.getKeyStoreValue();
//        }
//        catch (KeyStoreException e) {
//        }
//        try (InputStream inKeyStore = context.getKeyStorePathFile().isEmpty() ?
//                ClassLoader.getSystemResourceAsStream(KEY_STORE_DEFAULT_RESOURCE_PATH) : new FileInputStream(new File(context.getKeyStorePathFile()))) {
//            keyStoreServer = KeyStore.getInstance(context.getKeyStoreType());
//            keyStoreServer.load(inKeyStore, context.getKeyStorePasswordServer() == null ? null : context.getKeyStorePasswordServer().toCharArray());
//        } catch (Exception ex) {
//            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
//        }
//
//        context.setKeyStoreValue(keyStoreServer);
//        return keyStoreServer;
//    }

    public static List<ObjectModel> getModels(LwM2MTransportContextServer context) {
        if (context.getCtxServer().getModelsValue() != null && context.getCtxServer().getModelsValue().size() > 0) return context.getCtxServer().getModelsValue();
        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (context.getCtxServer().getModelPathFile() != null && !context.getCtxServer().getModelPathFile().isEmpty()) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(context.getCtxServer().getModelPathFile())));
        } else {
            try {
                List<ObjectModel> listModels = ObjectLoader.loadDdfResources(MODEL_DEFAULT_RESOURCE_PATH, modelPaths);
                models.addAll(listModels);
            } catch (java.lang.IllegalStateException e) {
                log.error(e.toString());
            }
        }
        context.getCtxServer().setModelsValue(models);
        return models;
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

    public JsonObject validateJson(String jsonStr) {
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

    /**
     * Equals to Map for values
     * @param map1
     * @param map2
     * @param <V>
     * @return
     */
    public static <V extends Comparable<V>>  boolean valuesEquals(Map<?,V> map1, Map<?,V> map2) {
        List<V> values1 = new ArrayList<V>(map1.values());
        List<V> values2 = new ArrayList<V>(map2.values());
        Collections.sort(values1);
        Collections.sort(values2);
        return values1.equals(values2);
    }
}
