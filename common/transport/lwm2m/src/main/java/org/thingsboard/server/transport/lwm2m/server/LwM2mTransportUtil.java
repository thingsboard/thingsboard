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
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.registration.Registration;
import org.nustaq.serialization.FSTConfiguration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;

@Slf4j
public class LwM2mTransportUtil {

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

    public static final String LOG_LW2M_TELEMETRY = "logLwm2m";
    public static final String LOG_LW2M_INFO = "info";
    public static final String LOG_LW2M_ERROR = "error";
    public static final String LOG_LW2M_WARN = "warn";
    public static final String LOG_LW2M_VALUE = "value";

    public static final int LWM2M_STRATEGY_1 = 1;
    public static final int LWM2M_STRATEGY_2 = 2;

    public static final String CLIENT_NOT_AUTHORIZED = "Client not authorized";
    public static final String LWM2M_VERSION_DEFAULT = "1.0";

    // RPC
    public static final String TYPE_OPER_KEY = "typeOper";
    public static final String TARGET_ID_VER_KEY = "targetIdVer";
    public static final String KEY_NAME_KEY = "key";
    public static final String VALUE_KEY = "value";
    public static final String PARAMS_KEY = "params";
    public static final String SEPARATOR_KEY = ":";
    public static final String FINISH_VALUE_KEY = ",";
    public static final String START_JSON_KEY = "{";
    public static final String FINISH_JSON_KEY = "}";
    //    public static final String contentFormatNameKey = "contentFormatName";
    public static final String INFO_KEY = "info";
    //    public static final String TIME_OUT_IN_MS = "timeOutInMs";
    public static final String RESULT_KEY = "result";
    public static final String ERROR_KEY = "error";
    public static final String METHOD_KEY = "methodName";

    // Firmware
    public static final String FW_UPDATE = "Firmware update";
    public static final Integer FW_ID = 5;
    // Package W
    public static final String FW_PACKAGE_ID = "/5/0/0";
    // State R
    public static final String FW_STATE_ID = "/5/0/3";
    // Update Result R
    public static final String FW_RESULT_ID = "/5/0/5";
    // PkgName R
    public static final String FW_NAME_ID = "/5/0/6";
    // PkgVersion R
    public static final String FW_VER_ID = "/5/0/7";
    // Update E
    public static final String FW_UPDATE_ID = "/5/0/2";

    // Software
    public static final String SW_UPDATE = "Software update";
    public static final Integer SW_ID = 9;
    // Package W
    public static final String SW_PACKAGE_ID = "/9/0/2";
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

    /**
     * Define the behavior of a write request.
     */
    public enum LwM2mTypeOper {
        /**
         * GET
         */
        READ(0, "Read"),
        DISCOVER(1, "Discover"),
        DISCOVER_ALL(2, "DiscoverAll"),
        OBSERVE_READ_ALL(3, "ObserveReadAll"),
        /**
         * POST
         */
        OBSERVE(4, "Observe"),
        OBSERVE_CANCEL(5, "ObserveCancel"),
        OBSERVE_CANCEL_ALL(6, "ObserveCancelAll"),
        EXECUTE(7, "Execute"),
        /**
         * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
         * section 5.3.3 of the LW M2M spec).
         * if all resources are to be replaced
         */
        WRITE_REPLACE(8, "WriteReplace"),
        /*
          PUT
         */
        /**
         * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
         * 5.3.3 of the LW M2M spec).
         * if this is a partial update request
         */
        WRITE_UPDATE(9, "WriteUpdate"),
        WRITE_ATTRIBUTES(10, "WriteAttributes"),
        DELETE(11, "Delete"),

        // only for RPC
        FW_UPDATE(12,"FirmwareUpdate");
//        FW_READ_INFO(12, "FirmwareReadInfo"),

//        SW_READ_INFO(15, "SoftwareReadInfo"),
//        SW_UPDATE(16, "SoftwareUpdate"),
//        SW_UNINSTALL(18, "SoftwareUninstall");

        public int code;
        public String type;

        LwM2mTypeOper(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2mTypeOper fromLwLwM2mTypeOper(String type) {
            for (LwM2mTypeOper to : LwM2mTypeOper.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported typeOper type  : %s", type));
        }
    }

    /**
     * /** State R
     * 0: Idle (before downloading or after successful updating)
     * 1: Downloading (The data sequence is on the way)
     * 2: Downloaded
     * 3: Updating
     */
    public enum StateFw {
        IDLE(0, "Idle"),
        DOWNLOADING(1, "Downloading"),
        DOWNLOADED(2, "Downloaded"),
        UPDATING(3, "Updating");

        public int code;
        public String type;

        StateFw(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static StateFw fromStateFwByType(String type) {
            for (StateFw to : StateFw.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported FW State type  : %s", type));
        }

        public static StateFw fromStateFwByCode(int code) {
            for (StateFw to : StateFw.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported FW State code : %s", code));
        }
    }

    /**
     * FW Update Result
     * 0: Initial value. Once the updating process is initiated (Download /Update), this Resource MUST be reset to Initial value.
     * 1: Firmware updated successfully.
     * 2: Not enough flash memory for the new firmware package.
     * 3: Out of RAM during downloading process.
     * 4: Connection lost during downloading process.
     * 5: Integrity check failure for new downloaded package.
     * 6: Unsupported package type.
     * 7: Invalid URI.
     * 8: Firmware update failed.
     * 9: Unsupported protocol.
     */
    public enum UpdateResultFw {
        INITIAL(0, "Initial value", false),
        UPDATE_SUCCESSFULLY(1, "Firmware updated successfully", false),
        NOT_ENOUGH(2, "Not enough flash memory for the new firmware package", false),
        OUT_OFF_MEMORY(3, "Out of RAM during downloading process", false),
        CONNECTION_LOST(4, "Connection lost during downloading process", true),
        INTEGRITY_CHECK_FAILURE(5, "Integrity check failure for new downloaded package", true),
        UNSUPPORTED_TYPE(6, "Unsupported package type", false),
        INVALID_URI(7, "Invalid URI", false),
        UPDATE_FAILED(8, "Firmware update failed", false),
        UNSUPPORTED_PROTOCOL(9, "Unsupported protocol", false);

        public int code;
        public String type;
        public boolean isAgain;

        UpdateResultFw(int code, String type, boolean isAgain) {
            this.code = code;
            this.type = type;
            this.isAgain = isAgain;
        }

        public static UpdateResultFw fromUpdateResultFwByType(String type) {
            for (UpdateResultFw to : UpdateResultFw.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported FW Update Result type  : %s", type));
        }

        public static UpdateResultFw fromUpdateResultFwByCode(int code) {
            for (UpdateResultFw to : UpdateResultFw.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported FW Update Result code  : %s", code));
        }
    }

    /**
     * FirmwareUpdateStatus {
     * DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
     */
    public static OtaPackageUpdateStatus EqualsFwSateToFirmwareUpdateStatus(StateFw stateFw, UpdateResultFw updateResultFw) {
        switch (updateResultFw) {
            case INITIAL:
                switch (stateFw) {
                    case IDLE:
                        return VERIFIED;
                    case DOWNLOADING:
                        return DOWNLOADING;
                    case DOWNLOADED:
                        return DOWNLOADED;
                    case UPDATING:
                        return UPDATING;
                }
            case UPDATE_SUCCESSFULLY:
                return UPDATED;
            case NOT_ENOUGH:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case INTEGRITY_CHECK_FAILURE:
            case UNSUPPORTED_TYPE:
            case INVALID_URI:
            case UPDATE_FAILED:
            case UNSUPPORTED_PROTOCOL:
                return FAILED;
            default:
                throw new CodecException("Invalid value stateFw %s   %s for FirmwareUpdateStatus.", stateFw.name(), updateResultFw.name());
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

    public static final String EVENT_AWAKE = "AWAKE";
    public static final String RESPONSE_REQUEST_CHANNEL = "RESP_REQ";
    public static final String RESPONSE_CHANNEL = "RESP";
    public static final String OBSERVE_CHANNEL = "OBSERVE";

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


    public static LwM2mClientProfile getNewProfileParameters(JsonObject profilesConfigData, TenantId tenantId) {
        LwM2mClientProfile lwM2MClientProfile = new LwM2mClientProfile();
        lwM2MClientProfile.setTenantId(tenantId);
        lwM2MClientProfile.setPostClientLwM2mSettings(profilesConfigData.get(CLIENT_LWM2M_SETTINGS).getAsJsonObject());
        lwM2MClientProfile.setPostKeyNameProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(KEY_NAME).getAsJsonObject());
        lwM2MClientProfile.setPostAttributeProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).getAsJsonArray());
        lwM2MClientProfile.setPostTelemetryProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).getAsJsonArray());
        lwM2MClientProfile.setPostObserveProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE_LWM2M).getAsJsonArray());
        lwM2MClientProfile.setPostAttributeLwm2mProfile(profilesConfigData.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE_LWM2M).getAsJsonObject());
        return lwM2MClientProfile;
    }

    /**
     * @return deviceProfileBody with Observe&Attribute&Telemetry From Thingsboard
     * Example:
     * property: {"clientLwM2mSettings": {
     * clientUpdateValueAfterConnect: false;
     * }
     * property: "observeAttr"
     * {"keyName": {
     * "/3/0/1": "modelNumber",
     * "/3/0/0": "manufacturer",
     * "/3/0/2": "serialNumber"
     * },
     * "attribute":["/2/0/1","/3/0/9"],
     * "telemetry":["/1/0/1","/2/0/1","/6/0/1"],
     * "observe":["/2/0","/2/0/0","/4/0/2"]}
     * "attributeLwm2m": {"/3_1.0": {"ver": "currentTimeTest11"},
     * "/3_1.0/0": {"gt": 17},
     * "/3_1.0/0/9": {"pmax": 45}, "/3_1.2": {ver": "3_1.2"}}
     */
    public static LwM2mClientProfile toLwM2MClientProfile(DeviceProfile deviceProfile) {
        if (((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties().size() > 0) {
            Object profile = ((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties();
            try {
                ObjectMapper mapper = new ObjectMapper();
                String profileStr = mapper.writeValueAsString(profile);
                JsonObject profileJson = (profileStr != null) ? validateJson(profileStr) : null;
                return getValidateCredentialsBodyFromThingsboard(profileJson) ? LwM2mTransportUtil.getNewProfileParameters(profileJson, deviceProfile.getTenantId()) : null;
            } catch (IOException e) {
                log.error("", e);
            }
        }
        return null;
    }

    public static JsonObject getBootstrapParametersFromThingsboard(DeviceProfile deviceProfile) {
        if (deviceProfile != null && ((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties().size() > 0) {
            Object bootstrap = ((Lwm2mDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration()).getProperties();
            try {
                ObjectMapper mapper = new ObjectMapper();
                String bootstrapStr = mapper.writeValueAsString(bootstrap);
                JsonObject objectMsg = (bootstrapStr != null) ? validateJson(bootstrapStr) : null;
                return (getValidateBootstrapProfileFromThingsboard(objectMsg)) ? objectMsg.get(BOOTSTRAP).getAsJsonObject() : null;
            } catch (IOException e) {
                log.error("", e);
            }
        }
        return null;
    }

    public static int getClientOnlyObserveAfterConnect(LwM2mClientProfile profile) {
        return profile.getPostClientLwM2mSettings().getAsJsonObject().has("clientOnlyObserveAfterConnect") ?
                profile.getPostClientLwM2mSettings().getAsJsonObject().get("clientOnlyObserveAfterConnect").getAsInt() : 1;
    }

    private static boolean getValidateCredentialsBodyFromThingsboard(JsonObject objectMsg) {
        return (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has(CLIENT_LWM2M_SETTINGS) &&
                !objectMsg.get(CLIENT_LWM2M_SETTINGS).isJsonNull() &&
                objectMsg.get(CLIENT_LWM2M_SETTINGS).isJsonObject() &&
                objectMsg.has(OBSERVE_ATTRIBUTE_TELEMETRY) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonObject() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(KEY_NAME) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(KEY_NAME).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(KEY_NAME).isJsonObject() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(ATTRIBUTE) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(TELEMETRY) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(OBSERVE_LWM2M) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE_LWM2M).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE_LWM2M).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(ATTRIBUTE_LWM2M) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE_LWM2M).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE_LWM2M).isJsonObject());
    }

    private static boolean getValidateBootstrapProfileFromThingsboard(JsonObject objectMsg) {
        return (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has(BOOTSTRAP) &&
                objectMsg.get(BOOTSTRAP).isJsonObject() &&
                !objectMsg.get(BOOTSTRAP).isJsonNull() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().has(SERVERS) &&
                !objectMsg.get(BOOTSTRAP).getAsJsonObject().get(SERVERS).isJsonNull() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().get(SERVERS).isJsonObject() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().has(BOOTSTRAP_SERVER) &&
                !objectMsg.get(BOOTSTRAP).getAsJsonObject().get(BOOTSTRAP_SERVER).isJsonNull() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().get(BOOTSTRAP_SERVER).isJsonObject() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().has(LWM2M_SERVER) &&
                !objectMsg.get(BOOTSTRAP).getAsJsonObject().get(LWM2M_SERVER).isJsonNull() &&
                objectMsg.get(BOOTSTRAP).getAsJsonObject().get(LWM2M_SERVER).isJsonObject());
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

    public static String convertPathFromIdVerToObjectId(String pathIdVer) {
        try {
            String[] keyArray = pathIdVer.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LWM2M_SEPARATOR_KEY).length == 2) {
                keyArray[1] = keyArray[1].split(LWM2M_SEPARATOR_KEY)[0];
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } else {
                return pathIdVer;
            }
        } catch (Exception e) {
            return null;
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
                return convertPathFromObjectIdToIdVer(pathIdVer, registration);
            }
        }
    }

    public static String convertPathFromObjectIdToIdVer(String path, Registration registration) {
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
    public static DownlinkRequest createWriteAttributeRequest(String target, Object params, DefaultLwM2MTransportMsgHandler serviceImpl) {
        AttributeSet attrSet = new AttributeSet(createWriteAttributes(params, serviceImpl, target));
        return attrSet.getAttributes().size() > 0 ? new WriteAttributesRequest(target, attrSet) : null;
    }

    private static Attribute[] createWriteAttributes(Object params, DefaultLwM2MTransportMsgHandler serviceImpl, String target) {
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

    public static Set<String> convertJsonArrayToSet(JsonArray jsonArray) {
        List<String> attributeListOld = new Gson().fromJson(jsonArray, new TypeToken<List<String>>() {
        }.getType());
        return Sets.newConcurrentHashSet(attributeListOld);
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

    public static LwM2mTypeOper setValidTypeOper(String typeOper) {
        try {
            return LwM2mTransportUtil.LwM2mTypeOper.fromLwLwM2mTypeOper(typeOper);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object convertWriteAttributes(String type, Object value, DefaultLwM2MTransportMsgHandler serviceImpl, String target) {
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
                if (value.getClass().getSimpleName().equals("String") ) {
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
            return  null;
        }
    }

    public static boolean isFwSwWords (String pathName) {
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
}
