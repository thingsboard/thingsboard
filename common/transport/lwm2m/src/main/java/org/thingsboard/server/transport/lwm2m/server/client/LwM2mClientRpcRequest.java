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
package org.thingsboard.server.transport.lwm2m.server.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2MTransportMsgHandler;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.ERROR_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FINISH_JSON_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FINISH_VALUE_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.INFO_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.KEY_NAME_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.DISCOVER_ALL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.EXECUTE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.FW_UPDATE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_CANCEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_READ_ALL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_ATTRIBUTES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_UPDATE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.METHOD_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.PARAMS_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.RESULT_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SEPARATOR_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.START_JSON_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.TARGET_ID_VER_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.VALUE_KEY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromIdVerToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.validPathIdVer;

@Slf4j
@Data
public class LwM2mClientRpcRequest {

    private Registration registration;
    private TransportProtos.SessionInfoProto sessionInfo;
    private String bodyParams;
    private int requestId;

    private LwM2mTypeOper typeOper;
    private String key;
    private String targetIdVer;
    private Object value;
    private Map<String, Object> params;

    private String errorMsg;
    private String valueMsg;
    private String infoMsg;
    private String responseCode;

    public LwM2mClientRpcRequest() {
    }

    public LwM2mClientRpcRequest(LwM2mTypeOper lwM2mTypeOper, String bodyParams, int requestId,
                                 TransportProtos.SessionInfoProto sessionInfo, Registration registration, DefaultLwM2MTransportMsgHandler handler) {
        this.registration = registration;
        this.sessionInfo = sessionInfo;
        this.requestId = requestId;
        if (lwM2mTypeOper != null) {
            this.typeOper = lwM2mTypeOper;
        } else {
            this.errorMsg = METHOD_KEY + " - " + typeOper + " is not valid.";
        }
        if (this.errorMsg == null &&  !bodyParams.equals("null")) {
            this.bodyParams = bodyParams;
            this.init(handler);
        }
    }

    public TransportProtos.ToDeviceRpcResponseMsg getDeviceRpcResponseResultMsg() {
        JsonObject payloadResp = new JsonObject();
        payloadResp.addProperty(RESULT_KEY, this.responseCode);
        if (this.errorMsg != null) {
            payloadResp.addProperty(ERROR_KEY, this.errorMsg);
        } else if (this.valueMsg != null) {
            payloadResp.addProperty(VALUE_KEY, this.valueMsg);
        } else if (this.infoMsg != null) {
            payloadResp.addProperty(INFO_KEY, this.infoMsg);
        }
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setPayload(payloadResp.getAsJsonObject().toString())
                .setRequestId(this.requestId)
                .build();
    }

    private void init(DefaultLwM2MTransportMsgHandler handler) {
        try {
            // #1
            if (this.bodyParams.contains(KEY_NAME_KEY)) {
                String targetIdVerStr = this.getValueKeyFromBody(KEY_NAME_KEY);
                if (targetIdVerStr != null) {
                    String targetIdVer = handler.getPresentPathIntoProfile(sessionInfo, targetIdVerStr);
                    if (targetIdVer != null) {
                        this.targetIdVer = targetIdVer;
                        this.setInfoMsg(String.format("Changed by: key - %s, pathIdVer - %s",
                                targetIdVerStr, targetIdVer));
                    }
                }
            }
            if (this.getTargetIdVer() == null && this.bodyParams.contains(TARGET_ID_VER_KEY)) {
                this.setValidTargetIdVerKey();
            }
            if (this.bodyParams.contains(VALUE_KEY)) {
                this.value = this.getValueKeyFromBody(VALUE_KEY);
            }
            try {
                if (this.bodyParams.contains(PARAMS_KEY)) {
                    this.setValidParamsKey(handler);
                }
            } catch (Exception e) {
                this.setErrorMsg(String.format("Params of request is bad Json format. %s", e.getMessage()));
            }

            if (this.getTargetIdVer() == null
                    && !(OBSERVE_READ_ALL == this.getTypeOper()
                    || DISCOVER_ALL == this.getTypeOper()
                    || OBSERVE_CANCEL == this.getTypeOper()
                    || FW_UPDATE == this.getTypeOper())) {
                this.setErrorMsg(TARGET_ID_VER_KEY + " and " +
                        KEY_NAME_KEY + " is null or bad format");
            }
            /**
             * EXECUTE && WRITE_REPLACE - only for Resource or ResourceInstance
             */
            else if (this.getTargetIdVer() != null
                    && (EXECUTE == this.getTypeOper()
                    || WRITE_REPLACE == this.getTypeOper())
                    && !(new LwM2mPath(Objects.requireNonNull(convertPathFromIdVerToObjectId(this.getTargetIdVer()))).isResource()
                    || new LwM2mPath(Objects.requireNonNull(convertPathFromIdVerToObjectId(this.getTargetIdVer()))).isResourceInstance())) {
                this.setErrorMsg("Invalid parameter " + TARGET_ID_VER_KEY
                        + ". Only Resource or ResourceInstance can be this operation");
            }
        } catch (Exception e) {
            this.setErrorMsg(String.format("Bad format request. %s", e.getMessage()));
        }

    }

    private void setValidTargetIdVerKey() {
        String targetIdVerStr = this.getValueKeyFromBody(TARGET_ID_VER_KEY);
        // targetIdVer without ver - ok
        try {
            // targetIdVer with/without ver - ok
            this.targetIdVer = validPathIdVer(targetIdVerStr, this.registration);
            if (this.targetIdVer != null) {
                this.infoMsg = String.format("Changed by: pathIdVer - %s", this.targetIdVer);
            }
        } catch (Exception e) {
            if (this.targetIdVer == null) {
                this.errorMsg = TARGET_ID_VER_KEY + " - " + targetIdVerStr + " is not valid.";
            }
        }
    }

    private void setValidParamsKey(DefaultLwM2MTransportMsgHandler handler) {
        String paramsStr = this.getValueKeyFromBody(PARAMS_KEY);
        if (paramsStr != null) {
            String params2Json =
                    START_JSON_KEY
                            + "\""
                            + paramsStr
                            .replaceAll(SEPARATOR_KEY, "\"" + SEPARATOR_KEY + "\"")
                            .replaceAll(FINISH_VALUE_KEY, "\"" + FINISH_VALUE_KEY + "\"")
                            + "\""
                            + FINISH_JSON_KEY;
            // jsonObject
            Map<String, Object> params = new Gson().fromJson(params2Json, new TypeToken<ConcurrentHashMap<String, Object>>() {
            }.getType());
            if (WRITE_UPDATE == this.getTypeOper()) {
                if (this.targetIdVer != null) {
                    Map<String, Object> paramsResourceId = this.convertParamsToResourceId((ConcurrentHashMap<String, Object>) params, handler);
                    if (paramsResourceId.size() > 0) {
                        this.setParams(paramsResourceId);
                    }
                }
            } else if (WRITE_ATTRIBUTES == this.getTypeOper()) {
                this.setParams(params);
            }
        }
    }

    private String getValueKeyFromBody(String key) {
        String valueKey = null;
        int startInd = -1;
        int finishInd = -1;
        try {
            switch (key) {
                case KEY_NAME_KEY:
                case TARGET_ID_VER_KEY:
                case VALUE_KEY:
                    startInd = this.bodyParams.indexOf(SEPARATOR_KEY, this.bodyParams.indexOf(key));
                    finishInd = this.bodyParams.indexOf(FINISH_VALUE_KEY, this.bodyParams.indexOf(key));
                    if (startInd >= 0 && finishInd < 0) {
                        finishInd = this.bodyParams.indexOf(FINISH_JSON_KEY, this.bodyParams.indexOf(key));
                    }
                    break;
                case PARAMS_KEY:
                    startInd = this.bodyParams.indexOf(START_JSON_KEY, this.bodyParams.indexOf(key));
                    finishInd = this.bodyParams.indexOf(FINISH_JSON_KEY, this.bodyParams.indexOf(key));
            }
            if (startInd >= 0 && finishInd > 0) {
                valueKey = this.bodyParams.substring(startInd + 1, finishInd);
            }
        } catch (Exception e) {
            log.error("", new TimeoutException());
        }
        /**
         * ReplaceAll "\""
         */
        if (StringUtils.trimToNull(valueKey) != null) {
            char[] chars = valueKey.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == 92 || chars[i] == 34) chars[i] = 32;
            }
            return key.equals(PARAMS_KEY) ? String.valueOf(chars) : String.valueOf(chars).replaceAll(" ", "");
        }
        return null;
    }

    private ConcurrentHashMap<String, Object> convertParamsToResourceId(ConcurrentHashMap<String, Object> params,
                                                                        DefaultLwM2MTransportMsgHandler serviceImpl) {
        Map<String, Object> paramsIdVer = new ConcurrentHashMap<>();
        LwM2mPath targetId = new LwM2mPath(Objects.requireNonNull(convertPathFromIdVerToObjectId(this.targetIdVer)));
        if (targetId.isObjectInstance()) {
            params.forEach((k, v) -> {
                try {
                    int id = Integer.parseInt(k);
                    paramsIdVer.put(String.valueOf(id), v);
                } catch (NumberFormatException e) {
                    String targetIdVer = serviceImpl.getPresentPathIntoProfile(sessionInfo, k);
                    if (targetIdVer != null) {
                        LwM2mPath lwM2mPath = new LwM2mPath(Objects.requireNonNull(convertPathFromIdVerToObjectId(targetIdVer)));
                            paramsIdVer.put(String.valueOf(lwM2mPath.getResourceId()), v);
                    }
                    /** WRITE_UPDATE*/
                    else {
                        String rezId = this.getRezIdByResourceNameAndObjectInstanceId(k, serviceImpl);
                        if (rezId != null) {
                            paramsIdVer.put(rezId, v);
                        }
                    }
                }
            });
        }
        return (ConcurrentHashMap<String, Object>) paramsIdVer;
    }

    private String getRezIdByResourceNameAndObjectInstanceId(String resourceName, DefaultLwM2MTransportMsgHandler handler) {
        LwM2mClient lwM2mClient = handler.clientContext.getClientBySessionInfo(this.sessionInfo);
        return lwM2mClient != null ?
                lwM2mClient.getRezIdByResourceNameAndObjectInstanceId(resourceName, this.targetIdVer, handler.config.getModelProvider()) :
                null;
    }
}
