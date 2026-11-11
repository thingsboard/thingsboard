// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.basic;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.http.AbstractHttpIntegration;
import org.thingsboard.server.common.msg.TbMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class BasicHttpIntegration<T extends HttpIntegrationMsg<?>> extends AbstractHttpIntegration<T> {

    private static final String HEADER = "Header:";

    private boolean securityEnabled = false;
    private boolean replaceNoContentToOk = false;

    private final Map<String, String> headersFilter = new HashMap<>();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        securityEnabled = json.has("enableSecurity") && json.get("enableSecurity").asBoolean();
        replaceNoContentToOk = json.has("replaceNoContentToOk") && json.get("replaceNoContentToOk").asBoolean();
        headersFilter.clear();
        if (securityEnabled && json.has("headersFilter")) {
            JsonNode headersFilterNode = json.get("headersFilter");
            for (Map.Entry<String, JsonNode> headerFilter : headersFilterNode.properties()) {
                headersFilter.put(headerFilter.getKey(), headerFilter.getValue().asText());
            }
        }
    }

    @Override
    protected ResponseEntity doProcess(T msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<String, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else {
                return processDownLinkData(context, result, msg);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    protected String getTypeUplink(T msg) {
        return "Uplink";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {
        logDownlink(context, "Downlink: " + msg.getTbMsg().getType(), msg);
        if (downlinkConverter != null) {
            context.putDownlinkMsg(msg);
        }
    }

    private ResponseEntity processDownLinkData(IntegrationContext context, Map<String, UplinkData> uplinkData, T msg) throws Exception {
        if (downlinkConverter != null) {
            List<TbMsg> tbMsgs = new ArrayList<>();
            for (String deviceName : uplinkData.keySet()) {
                DownLinkMsg pending = context.getDownlinkMsg(deviceName);
                if (pending != null && !pending.isEmpty()) {
                    tbMsgs.addAll(pending.getMsgs());
                }
            }
            Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
            msg.getRequestHeaders().forEach(
                    (header, value) -> {
                        mdMap.put("header:" + header, value);
                    }
            );

            List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), tbMsgs, new IntegrationMetaData(mdMap));

            for (String deviceName : uplinkData.keySet()) {
                context.removeDownlinkMsg(deviceName);
            }

            if (result.size() == 1 && !result.getFirst().isEmpty()) {
                DownlinkData downlink = result.getFirst();
                ResponseEntity response = switch (downlink.getContentType()) {
                    case "JSON" -> convertJson(downlink);
                    case "TEXT" -> convertText(downlink);
                    case "BINARY" -> convertBinary(downlink);
                    default -> throw new RuntimeException("Not supported content type: " + downlink.getContentType());
                };
                logDownlink(context, "Downlink", response);
                return response;
            }
        }

        return replaceNoContentToOk ? fromStatus(HttpStatus.OK) : fromStatus(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity convertJson(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "application/json");
        return new ResponseEntity<>(new String(downlink.getData(), StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
    }

    private ResponseEntity convertText(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "text/plain");
        return new ResponseEntity<>(new String(downlink.getData(), StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
    }

    private ResponseEntity convertBinary(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "application/octet-stream");
        return new ResponseEntity<>(downlink.getData(), responseHeaders, HttpStatus.OK);
    }

    private HttpHeaders getHttpHeaders(DownlinkData downlink, String defaultContentType) {
        HttpHeaders responseHeaders = new HttpHeaders();

        boolean contentTypePresent = false;
        for (Entry<String, String> kv : downlink.getMetadata().entrySet()) {
            if (kv.getKey().startsWith(HEADER)) {
                String headerName = kv.getKey().substring(HEADER.length());
                responseHeaders.add(headerName, kv.getValue());
                if (headerName.equalsIgnoreCase("Content-Type")) {
                    contentTypePresent = true;
                }
            }
        }
        if (!contentTypePresent) {
            responseHeaders.add("Content-Type", defaultContentType);
        }
        return responseHeaders;
    }

    protected Map<String, UplinkData> processUplinkData(IntegrationContext context, T msg) throws Exception {
        byte[] data = msg.getMsgInBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        msg.getRequestHeaders().forEach(
                (header, value) -> {
                    mdMap.put("Header:" + header, value);
                }
        );

        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, data, new UplinkMetaData<>(msg.getContentType(), mdMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            Map<String, UplinkData> result = new HashMap<>();
            for (UplinkData uplinkData : uplinkDataList) {
                processUplinkDataBlocking(context, uplinkData);
                result.put(uplinkData.getDeviceName(), uplinkData);
                log.trace("[{}] Processing uplink data", uplinkData);
            }
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    protected boolean checkSecurity(T msg) throws Exception {
        if (securityEnabled) {
            Map<String, String> requestHeaders = msg.getRequestHeaders();
            log.trace("Validating request using the following request headers: {}", requestHeaders);
            for (Map.Entry<String, String> headerFilter : headersFilter.entrySet()) {
                String value = requestHeaders.get(headerFilter.getKey().toLowerCase());
                if (value == null || !value.equals(headerFilter.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

}
