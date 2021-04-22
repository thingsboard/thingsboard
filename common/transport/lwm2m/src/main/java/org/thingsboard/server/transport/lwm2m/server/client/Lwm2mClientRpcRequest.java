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

import com.google.gson.JsonObject;
import lombok.Data;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LwM2mTypeOper;

import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.validPathIdVer;

@Data
public class Lwm2mClientRpcRequest {
    public final String targetIdVerKey = "targetIdVer";
    public final String keyNameKey = "key";
    public final String typeOperKey = "typeOper";
    public final String contentFormatNameKey = "contentFormatName";
    public final String valueKey = "value";
    public final String infoKey = "info";
    public final String paramsKey = "params";
    public final String timeoutInMsKey = "timeOutInMs";
    public final String resultKey = "result";
    public final String errorKey = "error";
    public final String methodKey = "methodName";

    private LwM2mTypeOper typeOper;
    private String targetIdVer;
     private String contentFormatName;
    private long timeoutInMs;
    private Object value;
    private ConcurrentHashMap<String, Object> params;
    private SessionInfoProto sessionInfo;
    private int requestId;
    private String errorMsg;
    private String valueMsg;
    private String infoMsg;
    private String responseCode;

    public void setValidTypeOper (String typeOper){
        try {
            this.typeOper = LwM2mTypeOper.fromLwLwM2mTypeOper(typeOper);
        } catch (Exception e) {
            this.errorMsg = this.methodKey + " - " + typeOper + " is not valid.";
        }
    }
    public void setValidContentFormatName (JsonObject rpcRequest){
        try {
            if (ContentFormat.fromName(rpcRequest.get(this.contentFormatNameKey).getAsString()) != null) {
                this.contentFormatName = rpcRequest.get(this.contentFormatNameKey).getAsString();
            }
            else {
                this.errorMsg = this.contentFormatNameKey + " -  " + rpcRequest.get(this.contentFormatNameKey).getAsString() + " is not valid.";
            }
        } catch (Exception e) {
            this.errorMsg = this.contentFormatNameKey + " - " + rpcRequest.get(this.contentFormatNameKey).getAsString() + " is not valid.";
        }
    }

    public void setValidTargetIdVerKey (JsonObject rpcRequest, Registration registration){
        if (rpcRequest.has(this.targetIdVerKey)) {
            String targetIdVerStr = rpcRequest.get(targetIdVerKey).getAsString();
            // targetIdVer without ver - ok
            try {
                // targetIdVer with/without ver - ok
                this.targetIdVer = validPathIdVer(targetIdVerStr, registration);
                if (this.targetIdVer != null){
                    this.infoMsg = String.format("Changed by: pathIdVer - %s", this.targetIdVer);
                }
            } catch (Exception e) {
                if (this.targetIdVer == null) {
                    this.errorMsg = this.targetIdVerKey + " - " + targetIdVerStr + " is not valid.";
                }
            }
        }
    }

    public TransportProtos.ToDeviceRpcResponseMsg getDeviceRpcResponseResultMsg() {
        JsonObject payloadResp = new JsonObject();
        payloadResp.addProperty(this.resultKey, this.responseCode);
        if (this.errorMsg != null) {
            payloadResp.addProperty(this.errorKey, this.errorMsg);
        }
        else if (this.valueMsg != null) {
            payloadResp.addProperty(this.valueKey, this.valueMsg);
        }
        else if (this.infoMsg != null) {
            payloadResp.addProperty(this.infoKey, this.infoMsg);
        }
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setPayload(payloadResp.getAsJsonObject().toString())
                .setRequestId(this.requestId)
                .build();
    }
}
