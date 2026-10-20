// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.TbMsg;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChirpStackIntegration extends BasicHttpIntegration<JsonHttpIntegrationMsg> {

    private static final String DEVICES_ENDPOINT = "/api/devices";
    private static final int MAX_ERROR_BODY_CHARS = 1024;
    private static final String DEV_EUI = "DevEUI";
    private static final String F_PORT = "fPort";
    private static final String DATA = "data";
    private static final String CONFIRMED = "confirmed";
    private static final String DEVICE_DOWNLINK_QUEUE_PARAMETER = "deviceQueueItem";
    private static final String DOWNLINK_QUEUE_PARAMETER = "queueItem";

    private boolean useAPI4Plus;

    private String applicationServerUrl = "";
    private String applicationServerAPIToken = "";
    private final RestTemplate httpClient = new RestTemplate();

    private String devicesUrl;


    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        JsonNode clientConfiguration = json.get("clientConfiguration");
        if (clientConfiguration.has("applicationServerAPIToken")) {
            applicationServerUrl = clientConfiguration.get("applicationServerUrl").asText();
            applicationServerAPIToken = clientConfiguration.get("applicationServerAPIToken").asText();
            useAPI4Plus = clientConfiguration.has("useAPI4Plus") && clientConfiguration.get("useAPI4Plus").asBoolean();
        }
        devicesUrl = applicationServerUrl + DEVICES_ENDPOINT;
    }

    private HttpEntity<JsonNode> createRequest(JsonNode deviceInfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(applicationServerAPIToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (deviceInfo != null) {
            return new HttpEntity<>(deviceInfo, headers);
        } else {
            return new HttpEntity<>(headers);
        }
    }

    @Override
    protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getMsgInBytes(), metadataTemplate);
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data", data);
            }
        }
        return fromStatus(HttpStatus.OK);
    }

    @Override
    protected String getTypeUplink(JsonHttpIntegrationMsg msg) {
        return "Uplink";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        if (StringUtils.isEmpty(this.applicationServerAPIToken)) {
            Exception e = new RuntimeException("Cannot send downlink because of Application Server API Token was not set.");
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
            return;
        }
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        try {
            List<DownlinkData> result = downlinkConverter.convertDownLink(
                    context.getDownlinkConverterContext(),
                    Collections.singletonList(msg),
                    new IntegrationMetaData(mdMap));
            if (!result.isEmpty()) {
                for (DownlinkData downlink : result) {
                    if (downlink.isEmpty()) {
                        continue;
                    }
                    Map<String, String> metadata = downlink.getMetadata();
                    if (!metadata.containsKey(DEV_EUI)) {
                        throw new ThingsboardException("DevEUI is missing in the downlink metadata!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                    }
                    if (!metadata.containsKey(F_PORT)) {
                        throw new ThingsboardException("FPort is missing in the downlink metadata!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                    }
                    String payload = new String(downlink.getData(), StandardCharsets.UTF_8);
                    ObjectNode body = createBodyForParameter(metadata, payload);
                    try {
                        httpClient.postForEntity(devicesUrl + "/" + metadata.get(DEV_EUI) + "/queue", createRequest(body), String.class);
                    } catch (HttpStatusCodeException e) {
                        throw new ThingsboardException(
                                "ChirpStack rejected downlink (" + e.getStatusCode() + "): " + StringUtils.truncate(e.getResponseBodyAsString(), MAX_ERROR_BODY_CHARS),
                                e.getStatusCode().is4xxClientError() ? ThingsboardErrorCode.BAD_REQUEST_PARAMS : ThingsboardErrorCode.GENERAL);
                    }
                    reportDownlinkOk(context, downlink);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
        }
    }

    private ObjectNode createBodyForParameter(Map<String, String> metadata, String payload) throws ThingsboardException {
        String downlinkQueueParameter = useAPI4Plus ? DOWNLINK_QUEUE_PARAMETER : DEVICE_DOWNLINK_QUEUE_PARAMETER;
        ObjectNode body = JacksonUtil.newObjectNode();
        ObjectNode queue = body.putObject(downlinkQueueParameter);
        if (metadata.containsKey(CONFIRMED)) {
            queue.put(CONFIRMED, parseConfirmed(metadata.get(CONFIRMED)));
        }
        queue.put(DATA, payload);
        queue.put(F_PORT, parseFPort(metadata.get(F_PORT)));
        return body;
    }

    private static int parseFPort(String raw) throws ThingsboardException {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ThingsboardException("FPort in the downlink metadata must be an integer, got: " + raw,
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private static boolean parseConfirmed(String raw) throws ThingsboardException {
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        throw new ThingsboardException("Confirmed in the downlink metadata must be 'true' or 'false', got: " + raw,
                ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

}
