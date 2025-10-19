/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.transport.lwm2m.config.TbLwM2mVersion;
import org.thingsboard.server.transport.lwm2m.server.LwM2mOtaConvert;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;
import org.thingsboard.server.transport.lwm2m.server.downlink.HasVersionedId;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_NODE_ID;
import static org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
import static org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
import static org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.common.transport.util.JsonUtils.convertToJsonObject;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_STATE_ID;


@Slf4j
public class LwM2MTransportUtil {

    public static final String LWM2M_OBJECT_VERSION_DEFAULT = "1.0";

    public static final String LOG_LWM2M_TELEMETRY = "transportLog";
    public static final String LOG_LWM2M_INFO = "info";
    public static final String LOG_LWM2M_ERROR = "error";
    public static final String LOG_LWM2M_WARN = "warn";
    public static final String REGISTRATION_TRIGGER_PARAMS_ID = "/1/0/8";
    public static final String BOOTSTRAP_TRIGGER_PARAMS_ID = "/1/0/9";;

    public static LwM2mOtaConvert convertOtaUpdateValueToString(String pathIdVer, Object value, ResourceModel.Type currentType) {
        String path = fromVersionedIdToObjectId(pathIdVer);
        LwM2mOtaConvert lwM2mOtaConvert = new LwM2mOtaConvert();
        if (path != null) {
            if (FW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateState.fromStateFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (FW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateResult.fromUpdateResultFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (SW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateState.fromUpdateStateSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (SW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateResult.fromUpdateResultSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            }
        }
        lwM2mOtaConvert.setCurrentType(currentType);
        lwM2mOtaConvert.setValue(value);
        return lwM2mOtaConvert;
    }

    public static Lwm2mDeviceProfileTransportConfiguration toLwM2MClientProfile(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.LWM2M)) {
            return (Lwm2mDeviceProfileTransportConfiguration) transportConfiguration;
        } else {
            log.info("[{}] Received profile with invalid transport configuration: {}", deviceProfile.getId(), deviceProfile.getProfileData().getTransportConfiguration());
            throw new IllegalArgumentException("Received profile with invalid transport configuration: " + transportConfiguration.getType());
        }
    }

    public static List<LwM2MBootstrapServerCredential> getBootstrapParametersFromThingsboard(DeviceProfile deviceProfile) {
        return toLwM2MClientProfile(deviceProfile).getBootstrap();
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
            log.debug("Issue converting path with version [{}] to path without version: ", pathIdVer, e);
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
                return keyArrayVer.length == 2 ? keyArrayVer[1] : LWM2M_OBJECT_VERSION_DEFAULT;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static String convertObjectIdToVersionedId(String path, LwM2mClient lwM2MClient) {
        String[] keyArray = path.split(LWM2M_SEPARATOR_PATH);
        if (keyArray.length > 1) {
            try {
                Integer objectId = Integer.valueOf((keyArray[1].split(LWM2M_SEPARATOR_KEY))[0]);
                String ver = String.valueOf(lwM2MClient.getSupportedObjectVersion(objectId));
                ver = ver != null ? ver : TbLwM2mVersion.VERSION_1_0.getVersion().toString();
                keyArray[1] = String.valueOf(keyArray[1]).contains(LWM2M_SEPARATOR_KEY) ? keyArray[1] : keyArray[1] + LWM2M_SEPARATOR_KEY + ver;
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } catch (Exception e) {
                return null;
            }
        } else {
            return path;
        }
    }

    /**
     * "UNSIGNED_INTEGER":  // Number -> Integer Example:
     * Alarm Timestamp [32-bit unsigned integer]
     * Short Server ID, Object ID, Object Instance ID, Resource ID, Resource Instance ID
     * "CORELINK": // String used in Attribute
     */
    public static ResourceModel.Type equalsResourceTypeGetSimpleName(Object value) {
        switch (value.getClass().getSimpleName()) {
            case "Float":
            case "Double":
                return FLOAT;
            case "Integer":
            case "Long":
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

    public static Object getJsonPrimitiveValue(JsonPrimitive value) {
        if (value.isString()) {
            return value.getAsString();
        } else if (value.isNumber()) {
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException i) {
                try {
                    return Long.valueOf(value.toString());
                } catch (NumberFormatException l) {
                    if (value.getAsFloat() >= Float.MIN_VALUE && value.getAsFloat() <= Float.MAX_VALUE) {
                        return value.getAsFloat();
                    } else {
                        return value.getAsDouble();
                    }
                }
            }
        } else if (value.isBoolean()) {
            return value.getAsBoolean();
        } else {
            return null;
        }
    }

    public static void validateVersionedId(LwM2mClient client, HasVersionedId request) {
        String msgExceptionStr = "";
        if (request.getObjectId() == null) {
            msgExceptionStr = "Specified object id is null!";
        } else {
            msgExceptionStr = client.isValidObjectVersion(request.getVersionedId());
        }
        if (!msgExceptionStr.isEmpty()) {
            throw new IllegalArgumentException(msgExceptionStr);
        }
    }

    public static Map<Integer, Object> convertMultiResourceValuesFromRpcBody(Object value, ResourceModel.Type type, String versionedId) throws Exception {
        if (value instanceof JsonElement) {
            return convertMultiResourceValuesFromJson((JsonElement) value, type, versionedId);
        } else if (value instanceof Map) {
            JsonElement valueConvert = convertToJsonObject((Map<String, ?>) value);
            return convertMultiResourceValuesFromJson(valueConvert, type, versionedId);
        } else {
            return null;
        }
    }

    public static Map<Integer, Object> convertMultiResourceValuesFromJson(JsonElement newValProto, ResourceModel.Type type, String versionedId) {
        Map<Integer, Object> newValues = new HashMap<>();
        newValProto.getAsJsonObject().entrySet().forEach((obj) -> {
            Object valueByTypeResource = convertValueByTypeResource(obj.getValue(), type, versionedId);
            newValues.put(Integer.valueOf(obj.getKey()), valueByTypeResource);
        });
        return newValues;
    }

    public static Object convertValueByTypeResource(Object value, ResourceModel.Type type, String versionedId) {
        Object valueCurrent = getJsonPrimitiveValue((JsonPrimitive) value);
        return LwM2mValueConverterImpl.getInstance().convertValue(valueCurrent,
                equalsResourceTypeGetSimpleName(valueCurrent), type, new LwM2mPath(fromVersionedIdToObjectId(versionedId)));
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

    @SuppressWarnings("unchecked")
    public static Optional<String> contentToString(Object content) {
        try {
            String value = null;
            LwM2mResource resource = null;
            String key = null;
            if (content instanceof Map) {
                Map<Object, Object> contentAsMap = (Map<Object, Object>) content;
                if (contentAsMap.size() == 1) {
                    for (Map.Entry<Object, Object> kv : contentAsMap.entrySet()) {
                        if (kv.getValue() instanceof LwM2mResource) {
                            key = kv.getKey().toString();
                            resource = (LwM2mResource) kv.getValue();
                        }
                    }
                }
            } else if (content instanceof LwM2mResource) {
                resource = (LwM2mResource) content;
            }
            if (resource != null && resource.getType() == OPAQUE) {
                value = opaqueResourceToString(resource, key);
            }
            value = value == null ? content.toString() : value;
            return Optional.of(value);
        } catch (Exception e) {
            log.debug("Failed to convert content " + content + " to string", e);
            return Optional.ofNullable(content != null ? content.toString() : null);
        }
    }

    private static String opaqueResourceToString(LwM2mResource resource, String key) {
        String value = null;
        StringBuilder builder = new StringBuilder();
        if (resource instanceof LwM2mSingleResource) {
            builder.append("LwM2mSingleResource");
            if (key == null) {
                builder.append(" id=").append(String.valueOf(resource.getId()));
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" value=").append(opaqueToString((byte[]) resource.getValue()));
            builder.append(" type=").append(OPAQUE.toString());
            value = builder.toString();
        } else if (resource instanceof LwM2mMultipleResource) {
            builder.append("LwM2mMultipleResource");
            if (key == null) {
                builder.append(" id=").append(String.valueOf(resource.getId()));
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" values={");
            if (resource.getInstances().size() > 0) {
                builder.append(multiInstanceOpaqueToString((LwM2mMultipleResource) resource));
            }
            builder.append("}");
            builder.append(" type=").append(OPAQUE.toString());
            value = builder.toString();
        }
        return value;
    }

    private static String multiInstanceOpaqueToString(LwM2mMultipleResource resource) {
        StringBuilder builder = new StringBuilder();
        resource.getInstances().values()
                .forEach(v -> builder.append(" id=").append(v.getId()).append(" value=").append(Hex.encodeHexString((byte[]) v.getValue())).append(", "));
        int startInd = builder.lastIndexOf(", ");
        if (startInd > 0) {
            builder.delete(startInd, startInd + 2);
        }
        return builder.toString();
    }

    private static String opaqueToString(byte[] value) {
        String opaque = Hex.encodeHexString(value);
        return opaque.length() > 1024 ? opaque.substring(0, 1024) : opaque;
    }

    public static LwM2mModel createModelsDefault() {
        return new StaticModel(ObjectLoader.loadDefault());
    }

    public static boolean compareAttNameKeyOta(String attrName) {
        for (OtaPackageKey value : OtaPackageKey.values()) {
            if (attrName.contains(value.getValue())) return true;
        }
        return false;
    }

    public static boolean valueEquals(Object newValue, Object oldValue) {
        String newValueStr;
        String oldValueStr;
        if (oldValue instanceof byte[]) {
            oldValueStr = Hex.encodeHexString((byte[]) oldValue);
        } else {
            oldValueStr = oldValue.toString();
        }
        if (newValue instanceof byte[]) {
            newValueStr = Hex.encodeHexString((byte[]) newValue);
        } else {
            newValueStr = newValue.toString();
        }
        return newValueStr.equals(oldValueStr);
    }

    public static void setDtlsConnectorConfigCidLength(Configuration serverCoapConfig, Integer cIdLength) {
        serverCoapConfig.setTransient(DTLS_CONNECTION_ID_LENGTH);
        serverCoapConfig.setTransient(DTLS_CONNECTION_ID_NODE_ID);
        serverCoapConfig.set(DTLS_CONNECTION_ID_LENGTH, cIdLength);
        if (cIdLength > 4) {
            serverCoapConfig.set(DTLS_CONNECTION_ID_NODE_ID, 0);
        } else {
            serverCoapConfig.set(DTLS_CONNECTION_ID_NODE_ID, null);
        }
    }

    public static int calculateSzx(int size) {
        if (size < 16 || size > 1024 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Size must be a power of 2 between 16 and 1024.");
        }
        return (int) (Math.log(size / 16) / Math.log(2));
    }

    public static ConcurrentHashMap<Integer, String[]> groupByObjectIdVersionedIds(Set<String> targetIds) {
        return targetIds.stream()
                .collect(Collectors.groupingBy(
                        id -> new LwM2mPath(fromVersionedIdToObjectId(id)).getObjectId(),
                        ConcurrentHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.toArray(new String[0])
                        )
                ));
    }

    public static boolean areArraysStringEqual(String[] oldValue, String[] newValue) {
        if (oldValue == null || newValue == null) return false;
        if (oldValue.length != newValue.length) return false;
        String[] sorted1 = oldValue.clone();
        String[] sorted2 = newValue.clone();
        Arrays.sort(sorted1);
        Arrays.sort(sorted2);
        return Arrays.equals(sorted1, sorted2);
    }

    public static ConcurrentHashMap<Integer, String[]> deepCopyConcurrentMap(Map<Integer, String[]> original) {
        return original.isEmpty() ? new ConcurrentHashMap<>() : original.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().clone() : null,
                        (v1, v2) -> v1, // merge function in case of duplicate keys
                        ConcurrentHashMap::new
                ));
    }

    public static boolean areMapsEqual(Map<Integer, String[]> m1, Map<Integer, String[]> m2) {
        if (m1.size() != m2.size()) return false;
        for (Integer key : m1.keySet()) {
            if (!m2.containsKey(key)) return false;

            String[] arr1 = m1.get(key);
            String[] arr2 = m2.get(key);

            if (arr1 == null || arr2 == null) {
                if (arr1 != arr2) return false;
                String[] sorted1 = arr1.clone();
                String[] sorted2 = arr2.clone();
                Arrays.sort(sorted1);
                Arrays.sort(sorted2);
                if (!Arrays.equals(sorted1, sorted2)) return false;
            }
        }
        return true;
    }
}
