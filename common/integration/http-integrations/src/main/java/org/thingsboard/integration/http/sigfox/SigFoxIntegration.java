// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.sigfox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class SigFoxIntegration extends BasicHttpIntegration<JsonHttpIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<String, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else if (result.size() > 1) {
                return fromStatus(HttpStatus.BAD_REQUEST);
            } else {
                Entry<String, UplinkData> entry = result.entrySet().stream().findFirst().get();
                String sigFoxDeviceId;
                String deviceIdAttributeName = metadataTemplate.getKvMap().get("SigFoxDeviceIdAttributeName");
                JsonNode msgBody = msg.getMsg();
                if (deviceIdAttributeName != null) {
                    if (msgBody.has(deviceIdAttributeName)) {
                        sigFoxDeviceId = msgBody.get(deviceIdAttributeName).asText();
                    } else {
                        throw new RuntimeException("Incoming message does not have the '" + deviceIdAttributeName + "' field!");
                    }
                } else {
                    if (msgBody.has("device")) {
                        sigFoxDeviceId = msgBody.get("device").asText();
                    } else if (msgBody.has("Device")) {
                        sigFoxDeviceId = msgBody.get("Device").asText();
                    } else {
                        throw new RuntimeException("Incoming message should contain either 'device' or 'Device' field!");
                    }
                }
                return processDownLinkData(context, entry.getKey(), msg, sigFoxDeviceId);
            }
        } else {
            return fromStatus(HttpStatus.FORBIDDEN);
        }
    }

    private ResponseEntity processDownLinkData(IntegrationContext context, String deviceName, JsonHttpIntegrationMsg msg, String sigFoxDeviceId) throws Exception {
        if (downlinkConverter != null) {
            DownLinkMsg pending = context.getDownlinkMsg(deviceName);
            if (pending != null && !pending.isEmpty()) {
                Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
                msg.getRequestHeaders().forEach(
                        (header, value) -> {
                            mdMap.put("header:" + header, value);
                        }
                );
                List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), pending.getMsgs(), new IntegrationMetaData(mdMap));
                context.removeDownlinkMsg(deviceName);
                if (result.size() == 1 && !result.getFirst().isEmpty()) {
                    DownlinkData downlink = result.getFirst();
                    ObjectNode json = JacksonUtil.newObjectNode();
                    json.putObject(sigFoxDeviceId).put("downlinkData", new String(downlink.getData(), StandardCharsets.UTF_8));
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.add("Content-Type", "application/json");
                    ResponseEntity<JsonNode> response = new ResponseEntity<>(json, responseHeaders, HttpStatus.OK);
                    logDownlink(context, "Downlink", response);
                    return response;
                }
            }
        }

        return fromStatus(HttpStatus.NO_CONTENT);
    }

}
