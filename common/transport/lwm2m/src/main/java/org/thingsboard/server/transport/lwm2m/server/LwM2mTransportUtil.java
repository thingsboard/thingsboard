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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.registration.Registration;
import org.nustaq.serialization.FSTConfiguration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.lwm2m.BootstrapConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2MUplinkMsgHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.leshan.core.attributes.Attribute.DIMENSION;
import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.OBJECT_VERSION;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;
import static org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
import static org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
import static org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;

@Slf4j
public class LwM2mTransportUtil {

    public static final String EVENT_AWAKE = "AWAKE";
    public static final String RESPONSE_REQUEST_CHANNEL = "RESP_REQ";
    public static final String RESPONSE_CHANNEL = "RESP";
    public static final String OBSERVE_CHANNEL = "OBSERVE";

    public static final String TRANSPORT_DEFAULT_LWM2M_VERSION = "1.0";
    public static final String CLIENT_LWM2M_SETTINGS = "clientLwM2mSettings";
    public static final String BOOTSTRAP = "bootstrap";
    public static final String SERVERS = "servers";
    public static final String LWM2M_SERVER = "lwm2mServer";
    public static final String BOOTSTRAP_SERVER = "bootstrapServer";
    public static final String OBSERVE_ATTRIBUTE_TELEMETRY = "observeAttr";
    public static final String ATTRIBUTE = "attribute";
    public static final String TELEMETRY = "telemetry";
    public static final String KEY_NAME = "keyName";
    public static final String OBSERVE_LWM2M = "observe";
    public static final String ATTRIBUTE_LWM2M = "attributeLwm2m";

    private static final String REQUEST = "/request";
    private static final String ATTRIBUTES = "/" + ATTRIBUTE;
    public static final String TELEMETRIES = "/" + TELEMETRY;
    public static final String ATTRIBUTES_REQUEST = ATTRIBUTES + REQUEST;
    public static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";

    public static final long DEFAULT_TIMEOUT = 2 * 60 * 1000L; // 2min in ms

    public static final String LOG_LWM2M_TELEMETRY = "logLwm2m";
    public static final String LOG_LWM2M_INFO = "info";
    public static final String LOG_LWM2M_ERROR = "error";
    public static final String LOG_LWM2M_WARN = "warn";
    public static final String LOG_LWM2M_VALUE = "value";

    public static final String CLIENT_NOT_AUTHORIZED = "Client not authorized";
    public static final String LWM2M_VERSION_DEFAULT = "1.0";

    // Firmware
    public static final String FIRMWARE_UPDATE_COAP_RECOURSE = "tbfw";
    public static final String FW_UPDATE = "Firmware update";
    public static final Integer FW_5_ID = 5;
    public static final Integer FW_19_ID = 19;

    // Package W
    public static final String FW_PACKAGE_5_ID = "/5/0/0";
    public static final String FW_PACKAGE_19_ID = "/19/0/0";
    // Package URI
    public static final String FW_PACKAGE_URI_ID = "/5/0/1";
    // State R
    public static final String FW_STATE_ID = "/5/0/3";
    // Update Result R
    public static final String FW_RESULT_ID = "/5/0/5";

    public static final String FW_DELIVERY_METHOD = "/5/0/9";

    // PkgName R
    public static final String FW_NAME_ID = "/5/0/6";
    // PkgVersion R
    public static final String FW_5_VER_ID = "/5/0/7";

    /**
     * Quectel@Hi15RM1-HLB_V1.0@BC68JAR01A10,V150R100C20B300SP7,V150R100C20B300SP7@8
     * BC68JAR01A10
     * # Request prodct type number
     * ATI
     * Quectel
     * BC68
     * Revision:BC68JAR01A10
     */
    public static final String FW_3_VER_ID = "/3/0/3";
    // Update E
    public static final String FW_UPDATE_ID = "/5/0/2";

    // Software
    public static final String SOFTWARE_UPDATE_COAP_RECOURSE = "softwareUpdateCoapRecourse";
    public static final String SW_UPDATE = "Software update";
    public static final Integer SW_ID = 9;
    // Package W
    public static final String SW_PACKAGE_ID = "/9/0/2";
    // Package URI
    public static final String SW_PACKAGE_URI_ID = "/9/0/3";
    // Update State R
    public static final String SW_UPDATE_STATE_ID = "/9/0/7";
    // Update Result R
    public static final String SW_RESULT_ID = "/9/0/9";
    // PkgName R
    public static final String SW_NAME_ID = "/9/0/0";
    // PkgVersion R
    public static final String SW_VER_ID = "/9/0/1";
    // Install E
    public static final String SW_INSTALL_ID = "/9/0/4";
    // Uninstall E
    public static final String SW_UN_INSTALL_ID = "/9/0/6";

    public enum LwM2mTypeServer {
        BOOTSTRAP(0, "bootstrap"),
        CLIENT(1, "client");

        public int code;
        public String type;

        LwM2mTypeServer(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2mTypeServer fromLwM2mTypeServer(String type) {
            for (LwM2mTypeServer sm : LwM2mTypeServer.values()) {
                if (sm.type.equals(type)) {
                    return sm;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported typeServer type : %d", type));
        }
    }

    public static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(UpdateResultFw updateResultFw) {
        switch (updateResultFw) {
            case INITIAL:
                return Optional.empty();
            case UPDATE_SUCCESSFULLY:
                return Optional.of(UPDATED);
            case NOT_ENOUGH:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case INTEGRITY_CHECK_FAILURE:
            case UNSUPPORTED_TYPE:
            case INVALID_URI:
            case UPDATE_FAILED:
            case UNSUPPORTED_PROTOCOL:
                return Optional.of(FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", updateResultFw.name());
        }
    }

    public static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(UpdateStateFw updateStateFw) {
        switch (updateStateFw) {
            case IDLE:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(DOWNLOADED);
            case UPDATING:
                return Optional.of(UPDATING);
            default:
                throw new CodecException("Invalid value stateFw %d for FirmwareUpdateStatus.", updateStateFw);
        }
    }

    /**
     * SW Update State R
     * 0: INITIAL Before downloading. (see 5.1.2.1)
     * 1: DOWNLOAD STARTED The downloading process has started and is on-going. (see 5.1.2.2)
     * 2: DOWNLOADED The package has been completely downloaded  (see 5.1.2.3)
     * 3: DELIVERED In that state, the package has been correctly downloaded and is ready to be installed.  (see 5.1.2.4)
     * If executing the Install Resource failed, the state remains at DELIVERED.
     * If executing the Install Resource was successful, the state changes from DELIVERED to INSTALLED.
     * After executing the UnInstall Resource, the state changes to INITIAL.
     * 4: INSTALLED
     */
    public enum UpdateStateSw {
        INITIAL(0, "Initial"),
        DOWNLOAD_STARTED(1, "DownloadStarted"),
        DOWNLOADED(2, "Downloaded"),
        DELIVERED(3, "Delivered"),
        INSTALLED(4, "Installed");

        public int code;
        public String type;

        UpdateStateSw(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static UpdateStateSw fromUpdateStateSwByType(String type) {
            for (UpdateStateSw to : UpdateStateSw.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", type));
        }

        public static UpdateStateSw fromUpdateStateSwByCode(int code) {
            for (UpdateStateSw to : UpdateStateSw.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", code));
        }
    }

    /**
     * SW Update Result
     * Contains the result of downloading or installing/uninstalling the software
     * 0: Initial value.
     * - Prior to download any new package in the Device, Update Result MUST be reset to this initial value.
     * - One side effect of executing the Uninstall resource is to reset Update Result to this initial value "0".
     * 1: Downloading.
     * - The package downloading process is on-going.
     * 2: Software successfully installed.
     * 3: Successfully Downloaded and package integrity verified
     * (( 4-49, for expansion, of other scenarios))
     * ** Failed
     * 50: Not enough storage for the new software package.
     * 51: Out of memory during downloading process.
     * 52: Connection lost during downloading process.
     * 53: Package integrity check failure.
     * 54: Unsupported package type.
     * 56: Invalid URI
     * 57: Device defined update error
     * 58: Software installation failure
     * 59: Uninstallation Failure during forUpdate(arg=0)
     * 60-200 : (for expansion, selection to be in blocks depending on new introduction of features)
     * This Resource MAY be reported by sending Observe operation.
     */
    public enum UpdateResultSw {
        INITIAL(0, "Initial value", false),
        DOWNLOADING(1, "Downloading", false),
        SUCCESSFULLY_INSTALLED(2, "Software successfully installed", false),
        SUCCESSFULLY_DOWNLOADED_VERIFIED(3, "Successfully Downloaded and package integrity verified", false),
        NOT_ENOUGH_STORAGE(50, "Not enough storage for the new software package", true),
        OUT_OFF_MEMORY(51, "Out of memory during downloading process", true),
        CONNECTION_LOST(52, "Connection lost during downloading process", false),
        PACKAGE_CHECK_FAILURE(53, "Package integrity check failure.", false),
        UNSUPPORTED_PACKAGE_TYPE(54, "Unsupported package type", false),
        INVALID_URI(56, "Invalid URI", true),
        UPDATE_ERROR(57, "Device defined update error", true),
        INSTALL_FAILURE(58, "Software installation failure", true),
        UN_INSTALL_FAILURE(59, "Uninstallation Failure during forUpdate(arg=0)", true);

        public int code;
        public String type;
        public boolean isAgain;

        UpdateResultSw(int code, String type, boolean isAgain) {
            this.code = code;
            this.type = type;
            this.isAgain = isAgain;
        }

        public static UpdateResultSw fromUpdateResultSwByType(String type) {
            for (UpdateResultSw to : UpdateResultSw.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported SW Update Result type  : %s", type));
        }

        public static UpdateResultSw fromUpdateResultSwByCode(int code) {
            for (UpdateResultSw to : UpdateResultSw.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported SW Update Result code  : %s", code));
        }
    }

    public enum LwM2MClientStrategy {
        CLIENT_STRATEGY_1(1, "Read only resources marked as observation"),
        CLIENT_STRATEGY_2(2, "Read all client resources");

        public int code;
        public String type;

        LwM2MClientStrategy(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2MClientStrategy fromStrategyClientByType(String type) {
            for (LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy type  : %s", type));
        }

        public static LwM2MClientStrategy fromStrategyClientByCode(int code) {
            for (LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy code : %s", code));
        }
    }

    /**
     * FirmwareUpdateStatus {
     * DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
     */
    public static OtaPackageUpdateStatus EqualsSwSateToFirmwareUpdateStatus(UpdateStateSw updateStateSw, UpdateResultSw updateResultSw) {
        switch (updateResultSw) {
            case INITIAL:
                switch (updateStateSw) {
                    case INITIAL:
                    case DOWNLOAD_STARTED:
                        return DOWNLOADING;
                    case DOWNLOADED:
                        return DOWNLOADED;
                    case DELIVERED:
                        return VERIFIED;
                }
            case DOWNLOADING:
                return DOWNLOADING;
            case SUCCESSFULLY_INSTALLED:
                return UPDATED;
            case SUCCESSFULLY_DOWNLOADED_VERIFIED:
                return VERIFIED;
            case NOT_ENOUGH_STORAGE:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case PACKAGE_CHECK_FAILURE:
            case UNSUPPORTED_PACKAGE_TYPE:
            case INVALID_URI:
            case UPDATE_ERROR:
            case INSTALL_FAILURE:
            case UN_INSTALL_FAILURE:
                return FAILED;
            default:
                throw new CodecException("Invalid value stateFw %s   %s for FirmwareUpdateStatus.", updateStateSw.name(), updateResultSw.name());
        }
    }


    public static boolean equalsResourceValue(Object valueOld, Object valueNew, ResourceModel.Type type, LwM2mPath
            resourcePath) throws CodecException {
        switch (type) {
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
                return String.valueOf(valueOld).equals(String.valueOf(valueNew));
            case TIME:
                return ((Date) valueOld).getTime() == ((Date) valueNew).getTime();
            case STRING:
            case OBJLNK:
                return valueOld.equals(valueNew);
            case OPAQUE:
                return Arrays.equals(Hex.decodeHex(((String) valueOld).toCharArray()), Hex.decodeHex(((String) valueNew).toCharArray()));
            default:
                throw new CodecException("Invalid value type for resource %s, type %s", resourcePath, type);
        }
    }

    public static LwM2mOtaConvert convertOtaUpdateValueToString(String pathIdVer, Object value, ResourceModel.Type currentType) {
        String path = fromVersionedIdToObjectId(pathIdVer);
        LwM2mOtaConvert lwM2mOtaConvert = new LwM2mOtaConvert();
        if (path != null) {
            if (FW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(UpdateStateFw.fromStateFwByCode(((Long) value).intValue()).type);
                return lwM2mOtaConvert;
            } else if (FW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(UpdateResultFw.fromUpdateResultFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (SW_UPDATE_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(UpdateStateSw.fromUpdateStateSwByCode(((Long) value).intValue()).type);
                return lwM2mOtaConvert;
            } else if (SW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(UpdateResultSw.fromUpdateResultSwByCode(((Long) value).intValue()).type);
                return lwM2mOtaConvert;
            }
        }
        lwM2mOtaConvert.setCurrentType(currentType);
        lwM2mOtaConvert.setValue(value);
        return lwM2mOtaConvert;
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

//    public static LwM2mClientProfile getNewProfileParameters(JsonObject profilesConfigData, TenantId tenantId) {
//        LwM2mClientProfile lwM2MClientProfile = new LwM2mClientProfile();
//        lwM2MClientProfile.setTenantId(tenantId);
//        lwM2MClientProfile.setPostClientLwM2mSettings(profilesConfigData.get(CLIENT_LWM2M_SETTINGS).getAsJsonObject());
//        lwM2MClientProfile.setPostKeyNameProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(KEY_NAME).getAsJsonObject());
//        lwM2MClientProfile.setPostAttributeProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).getAsJsonArray());
//        lwM2MClientProfile.setPostTelemetryProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).getAsJsonArray());
//        lwM2MClientProfile.setPostObserveProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE_LWM2M).getAsJsonArray());
//        lwM2MClientProfile.setPostAttributeLwm2mProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE_LWM2M).getAsJsonObject());
//        return lwM2MClientProfile;
//    }

    public static Lwm2mDeviceProfileTransportConfiguration toLwM2MClientProfile(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.LWM2M)) {
            return (Lwm2mDeviceProfileTransportConfiguration) transportConfiguration;
        } else {
            log.warn("[{}] Received profile with invalid transport configuration: {}", deviceProfile.getId(), deviceProfile.getProfileData().getTransportConfiguration());
            throw new IllegalArgumentException("Received profile with invalid transport configuration: " + transportConfiguration.getType());
        }
    }

    public static BootstrapConfiguration getBootstrapParametersFromThingsboard(DeviceProfile deviceProfile) {
        return toLwM2MClientProfile(deviceProfile).getBootstrap();
    }

    public static JsonObject validateJson(String jsonStr) {
        JsonObject object = null;
        if (jsonStr != null && !jsonStr.isEmpty()) {
            String jsonValidFlesh = jsonStr.replaceAll("\\\\", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\n", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\t", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll(" ", "");
            String jsonValid = (jsonValidFlesh.charAt(0) == '"' && jsonValidFlesh.charAt(jsonValidFlesh.length() - 1) == '"') ? jsonValidFlesh.substring(1, jsonValidFlesh.length() - 1) : jsonValidFlesh;
            try {
                object = new JsonParser().parse(jsonValid).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("[{}] Fail validateJson [{}]", jsonStr, e.getMessage());
            }
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> decode(byte[] byteArray) {
        try {
            FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();
            T msg = (T) config.asObject(byteArray);
            return Optional.ofNullable(msg);
        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
            return Optional.empty();
        }
    }

    public static String splitCamelCaseString(String s) {
        LinkedList<String> linkedListOut = new LinkedList<>();
        LinkedList<String> linkedList = new LinkedList<String>((Arrays.asList(s.split(" "))));
        linkedList.forEach(str -> {
            String strOut = str.replaceAll("\\W", "").replaceAll("_", "").toUpperCase();
            if (strOut.length() > 1) linkedListOut.add(strOut.charAt(0) + strOut.substring(1).toLowerCase());
            else linkedListOut.add(strOut);
        });
        linkedListOut.set(0, (linkedListOut.get(0).substring(0, 1).toLowerCase() + linkedListOut.get(0).substring(1)));
        return StringUtils.join(linkedListOut, "");
    }

    public static <T> TransportServiceCallback<Void> getAckCallback(LwM2mClient lwM2MClient,
                                                                    int requestId, String typeTopic) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] [{}] - requestId [{}] - EndPoint  , Access AckCallback", typeTopic, requestId, lwM2MClient.getEndpoint());
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg", e.toString());
            }
        };
    }

    public static String fromVersionedIdToObjectId(String pathIdVer) {
        try {
            if (pathIdVer == null) {
                return null;
            }
            String[] keyArray = pathIdVer.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LWM2M_SEPARATOR_KEY).length == 2) {
                keyArray[1] = keyArray[1].split(LWM2M_SEPARATOR_KEY)[0];
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } else {
                return pathIdVer;
            }
        } catch (Exception e) {
            log.warn("Issue converting path with version [{}] to path without version: ", pathIdVer, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path - pathId or pathIdVer
     * @return
     */
    public static String getVerFromPathIdVerOrId(String path) {
        try {
            String[] keyArray = path.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                String[] keyArrayVer = keyArray[1].split(LWM2M_SEPARATOR_KEY);
                return keyArrayVer.length == 2 ? keyArrayVer[1] : null;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static String validPathIdVer(String pathIdVer, Registration registration) throws
            IllegalArgumentException {
        if (!pathIdVer.contains(LWM2M_SEPARATOR_PATH)) {
            throw new IllegalArgumentException(String.format("Error:"));
        } else {
            String[] keyArray = pathIdVer.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LWM2M_SEPARATOR_KEY).length == 2) {
                return pathIdVer;
            } else {
                LwM2mPath pathObjId = new LwM2mPath(pathIdVer);
                return convertObjectIdToVersionedId(pathIdVer, registration);
            }
        }
    }

    public static String convertObjectIdToVersionedId(String path, Registration registration) {
        String ver = registration.getSupportedObject().get(new LwM2mPath(path).getObjectId());
        ver = ver != null ? ver : LWM2M_VERSION_DEFAULT;
        try {
            String[] keyArray = path.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                keyArray[1] = keyArray[1] + LWM2M_SEPARATOR_KEY + ver;
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } else {
                return path;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String validateObjectVerFromKey(String key) {
        try {
            return (key.split(LWM2M_SEPARATOR_PATH)[1].split(LWM2M_SEPARATOR_KEY)[1]);
        } catch (Exception e) {
            return ObjectModel.DEFAULT_VERSION;
        }
    }

    /**
     * As example:
     * a)Write-Attributes/3/0/9?pmin=1 means the Battery Level value will be notified
     * to the Server with a minimum interval of 1sec;
     * this value is set at theResource level.
     * b)Write-Attributes/3/0/9?pmin means the Battery Level will be notified
     * to the Server with a minimum value (pmin) given by the default one
     * (resource 2 of Object Server ID=1),
     * or with another value if this Attribute has been set at another level
     * (Object or Object Instance: see section5.1.1).
     * c)Write-Attributes/3/0?pmin=10 means that all Resources of Instance 0 of the Object ‘Device (ID:3)’
     * will be notified to the Server with a minimum interval of 10 sec;
     * this value is set at the Object Instance level.
     * d)Write-Attributes /3/0/9?gt=45&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 20 and new value is 35 due to step condition
     * b.old value is 45 and new value is 50 due to gt condition
     * c.old value is 50 and new value is 40 due to both gt and step conditions
     * d.old value is 35 and new value is 20 due to step conditione)
     * Write-Attributes /3/0/9?lt=20&gt=85&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 17 and new value is 24 due to lt condition
     * b.old value is 75 and new value is 90 due to both gt and step conditions
     * String uriQueries = "pmin=10&pmax=60";
     * AttributeSet attributes = AttributeSet.parse(uriQueries);
     * WriteAttributesRequest request = new WriteAttributesRequest(target, attributes);
     * Attribute gt = new Attribute(GREATER_THAN, Double.valueOf("45"));
     * Attribute st = new Attribute(LESSER_THAN, Double.valueOf("10"));
     * Attribute pmax = new Attribute(MAXIMUM_PERIOD, "60");
     * Attribute [] attrs = {gt, st};
     */
    public static SimpleDownlinkRequest createWriteAttributeRequest(String target, Object params, DefaultLwM2MUplinkMsgHandler serviceImpl) {
        AttributeSet attrSet = new AttributeSet(createWriteAttributes(params, serviceImpl, target));
        return attrSet.getAttributes().size() > 0 ? new WriteAttributesRequest(target, attrSet) : null;
    }

    private static Attribute[] createWriteAttributes(Object params, DefaultLwM2MUplinkMsgHandler serviceImpl, String target) {
        List<Attribute> attributeLists = new ArrayList<>();
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(params, ConcurrentHashMap.class);
        map.forEach((k, v) -> {
            if (StringUtils.trimToNull(v.toString()) != null) {
                Object attrValue = convertWriteAttributes(k, v, serviceImpl, target);
                if (attrValue != null) {
                    Attribute attribute = createAttribute(k, attrValue);
                    if (attribute != null) {
                        attributeLists.add(new Attribute(k, attrValue));
                    }
                }
            }
        });
        return attributeLists.toArray(Attribute[]::new);
    }

    public static ResourceModel.Type equalsResourceTypeGetSimpleName(Object value) {
        switch (value.getClass().getSimpleName()) {
            case "Double":
                return FLOAT;
            case "Integer":
                return INTEGER;
            case "String":
                return STRING;
            case "Boolean":
                return BOOLEAN;
            case "byte[]":
                return OPAQUE;
            case "Date":
                return TIME;
            case "ObjectLink":
                return OBJLNK;
            default:
                return null;
        }
    }

    public static Object convertWriteAttributes(String type, Object value, DefaultLwM2MUplinkMsgHandler serviceImpl, String target) {
        switch (type) {
            /** Integer [0:255]; */
            case DIMENSION:
                Long dim = (Long) serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
                return dim >= 0 && dim <= 255 ? dim : null;
            /**String;*/
            case OBJECT_VERSION:
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), STRING, new LwM2mPath(target));
            /**INTEGER */
            case MINIMUM_PERIOD:
            case MAXIMUM_PERIOD:
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
            /**Float; */
            case GREATER_THAN:
            case LESSER_THAN:
            case STEP:
                if (value.getClass().getSimpleName().equals("String")) {
                    value = Double.valueOf((String) value);
                }
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), FLOAT, new LwM2mPath(target));
            default:
                return null;
        }
    }

    private static Attribute createAttribute(String key, Object attrValue) {
        try {
            return new Attribute(key, attrValue);
        } catch (Exception e) {
            log.error("CreateAttribute, not valid parameter key: [{}], attrValue: [{}], error: [{}]", key, attrValue, e.getMessage());
            return null;
        }
    }

    public static boolean isFwSwWords(String pathName) {
        return OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.CHECKSUM).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.CHECKSUM_ALGORITHM).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.SIZE).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.CHECKSUM).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.CHECKSUM_ALGORITHM).equals(pathName)
                || OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.SIZE).equals(pathName);
    }

    /**
     * @param lwM2MClient -
     * @param path        -
     * @return - return value of Resource by idPath
     */
    public static LwM2mResource getResourceValueFromLwM2MClient(LwM2mClient lwM2MClient, String path) {
        LwM2mResource lwm2mResourceValue = null;
        ResourceValue resourceValue = lwM2MClient.getResources().get(path);
        if (resourceValue != null) {
            if (new LwM2mPath(fromVersionedIdToObjectId(path)).isResource()) {
                lwm2mResourceValue = lwM2MClient.getResources().get(path).getLwM2mResource();
            }
        }
        return lwm2mResourceValue;
    }
}
