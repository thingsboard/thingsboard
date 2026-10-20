// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.http.AbstractHttpIntegration;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ThingParkIntegrationEnterprise extends AbstractHttpIntegration<ThingParkIntegrationMsg> {

    private static final ThreadLocal<SimpleDateFormat> ISO8601 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    private static final String DEFAULT_DOWNLINK_URL = "https://api.thingpark.com/thingpark/lrc/rest/downlink";
    private static final String URL_PREFIX = "urlPrefix";
    private static final String DEFAULT_URL_PREFIX = "/core/latest/api/devices/";
    private static final String URL_SUFFIX = "urlSuffix";
    private static final String DEFAULT_URL_SUFFIX = "/downlinkMessages";
    private static final String URL_SUFFIX_TOKEN = "urlSuffixToken";
    private static final String DEFAULT_URL_SUFFIX_TOKEN = "/admin/latest/api/oauth/token";
    private static final String URL_SUFFIX_GET_DEVICES = "urlSuffixGetDevices";
    private static final String DEFAULT_URL_SUFFIX_GET_DEVICES = "/core/latest/api/devices";
    private static final String FIRST_PARAM_TOKEN = "firstParamToken";
    private static final String DEFAULT_FIRST_PARAM_TOKEN = "client_credentials";
    private static final String FIRST_PARAM_NAME_TOKEN = "?grant_type=";
    private static final String SECOND_PARAM_NAME_TOKEN = "&client_id=";
    private static final String THIRD_PARAM_NAME_TOKEN = "&client_secret=";
    private static final String DEV_EUI = "DevEUI";
    private static final String PAYLOAD = "payloadHex";
    private static final String F_PORT = "targetPorts";
    private static final String SECURITY_PARAMS = "securityParams";
    private static final String CREATION_TIME = "creationTime";
    private static final String AS_KEY = "asKey";
    private static final String AS_ID = "asId";
    private static final String DEFAULT_DEV_EUI_SENT = "DevEUI_downlink_Sent";
    private static final String DEFAULT_DEV_EUI_SENT_POS = "0";
    private static final String ERROR_STATUS = "ERROR";

    private long expiresInSeconds;
    private String tokenType;
    private String accessToken;
    private String securityAsId;
    private String securityAsKey;
    private String securityClientId;
    private String securityClientSecret;
    private String downlinkUrl;
    private int targetPorts;
    private String urlSuffixToken;
    private String firstParamToken;
    private String devEUiSent;
    private String devEUiSentPos;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        // Load security credentials for uplink validation and downlink security params
        // Check which security mode is active: enableSecurityNew determines which AS ID field to use
        boolean enableSecurityNew = json.has("enableSecurityNew") && json.get("enableSecurityNew").asBoolean();
        if (enableSecurityNew) {
            // New security mode: use asIdNew
            if (json.hasNonNull(AS_ID + "New")) {
                this.securityAsId = json.get(AS_ID + "New").asText();
            }
        } else {
            // Old security mode: use asId
            if (json.hasNonNull(AS_ID)) {
                this.securityAsId = json.get(AS_ID).asText();
            }
        }
        if (json.hasNonNull(AS_KEY)) {
            this.securityAsKey = json.get(AS_KEY).asText();
        }
        // Load downlink configuration
        if (downlinkConverter != null) {
            // OAuth credentials for downlink token authentication (only in new security mode)
            if (json.hasNonNull("clientIdNew")) {
                this.securityClientId = json.get("clientIdNew").asText();
            }
            if (json.hasNonNull("clientSecret")) {
                this.securityClientSecret = json.get("clientSecret").asText();
            }
            if (json.hasNonNull("downlinkUrl")) {
                this.downlinkUrl = json.get("downlinkUrl").asText();
            } else {
                this.downlinkUrl = DEFAULT_DOWNLINK_URL;
            }
            if (json.hasNonNull("devEUiSent")) {
                this.devEUiSent = json.get("devEUiSent").asText();
            } else {
                this.devEUiSent = DEFAULT_DEV_EUI_SENT;
            }
            if (json.hasNonNull("devEUiSentPos")) {
                this.devEUiSentPos = json.get("devEUiSentPos").asText();
            } else {
                this.devEUiSentPos = DEFAULT_DEV_EUI_SENT_POS;
            }
        }
    }

    @Override
    protected ResponseEntity doProcess(ThingParkIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg);
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkDataBlocking(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            return fromStatus(HttpStatus.OK);
        } else {
            return fromStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    protected String getTypeUplink(ThingParkIntegrationMsg msg) {
        return (msg != null && msg.getMsg() != null && msg.getMsg() instanceof JsonNode && msg.getMsg().has("DevEUI_downlink_Sent")) ?
                "Downlink_Sent" :
                "Uplink";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) processDownLinkMsg(context, msg);
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        String status;
        Exception exception;
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
                        throw new RuntimeException("DevEUI is missing in the downlink metadata!");
                    }
                    String dataStr = new String(downlink.getData(), StandardCharsets.UTF_8);
                    JsonNode dataJson = JacksonUtil.toJsonNode(dataStr);
                    urlSuffixToken = dataJson.has(URL_SUFFIX_TOKEN) ? dataJson.get(URL_SUFFIX_TOKEN).asText() : DEFAULT_URL_SUFFIX_TOKEN;
                    firstParamToken = dataJson.has(FIRST_PARAM_TOKEN) ? dataJson.get(FIRST_PARAM_TOKEN).asText() : DEFAULT_FIRST_PARAM_TOKEN;
                    processGetRestToken(downlink, context, msg);
                    String deviceName = dataJson.get(DEV_EUI).asText();
                    String urlSuffixGetDevices = dataJson.has(URL_SUFFIX_GET_DEVICES) ? dataJson.get(URL_SUFFIX_GET_DEVICES).asText() : DEFAULT_URL_SUFFIX_GET_DEVICES;
                    String ref = getRef(deviceName, urlSuffixGetDevices, downlink, msg);
                    String payloadHex = dataJson.get("payload").asText();
                    String urlPrefix = dataJson.has(URL_PREFIX) ? dataJson.get(URL_PREFIX).asText() : DEFAULT_URL_PREFIX;
                    String urlSuffix = dataJson.has(URL_SUFFIX) ? dataJson.get(URL_SUFFIX).asText() : DEFAULT_URL_SUFFIX;
                    sendMessage(ref, urlPrefix, urlSuffix, payloadHex, downlink, msg);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, ERROR_STATUS, e);
        }
    }

    private List<UplinkData> convertToUplinkDataList(IntegrationContext context, ThingParkIntegrationMsg msg) throws Exception {
        byte[] data = msg.getMsgInBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        ThingParkRequestParameters params = msg.getParams();
        mdMap.put("AS_ID", params.getAsId());
        mdMap.put("LrnDevEui", params.getLrnDevEui());
        mdMap.put("LrnFPort", params.getLrnFPort());
        return convertToUplinkDataList(context, data, new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
    }

    protected void processGetRestToken(DownlinkData downlink, IntegrationContext context, TbMsg msg) {
        long curTime = (new Date()).getTime();
        if (accessToken == null || accessToken.isEmpty() || expiresInSeconds < curTime) {
            String url = this.downlinkUrl + urlSuffixToken +
                    FIRST_PARAM_NAME_TOKEN + firstParamToken +
                    SECOND_PARAM_NAME_TOKEN + securityClientId +
                    THIRD_PARAM_NAME_TOKEN + securityClientSecret;
            ResponseEntity<String> response = sendRestHttp(url, HttpMethod.POST, "", false, msg);
            if (response != null) {
                setTokenValue(response);
            }
        }
    }

    private void setTokenValue(ResponseEntity<String> responseEntity) {
        String body = responseEntity.getBody();
        try {
            JsonNode tokenJson = JacksonUtil.toJsonNode(body);
            this.expiresInSeconds = (new Date()).getTime() + tokenJson.get("expires_in").asLong() * 1000;
            this.tokenType = tokenJson.get("token_type").asText();
            this.accessToken = tokenJson.get("access_token").asText();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private String getRef(String deviceName, String urlSuffix, DownlinkData downlink, TbMsg msg) {
        String url = this.downlinkUrl + urlSuffix;
        ResponseEntity<String> response = getResult(url, HttpMethod.GET, "", true, downlink, msg);
        String ref = "";
        if (response != null) {
            if (response.getStatusCode().is2xxSuccessful()) {
                try {
                    String bodyStr = response.getBody();
                    JsonNode body = JacksonUtil.toJsonNode(bodyStr);
                    ObjectReader reader = JacksonUtil.OBJECT_MAPPER.readerFor(new TypeReference<List<JsonNode>>() {
                    });
                    List<JsonNode> listDev = reader.readValue(body);
                    ref = (listDev.stream()
                            .filter(o -> o.get("EUI").asText().equals(deviceName))
                            .findFirst()
                            .get())
                            .get("ref").asText();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ref;
    }

    private void sendMessage(String ref, String urlPrefix, String urlSuffix, String payloadHex, DownlinkData downlink, TbMsg msg) {
        String url = this.downlinkUrl + urlPrefix + ref + urlSuffix;
        this.targetPorts = (this.targetPorts >= 15) ? 1 : this.targetPorts + 1;
        String creationTime = ISO8601.get().format(new Date());
        String body = "{" +
                "\"" + PAYLOAD + "\": \"" + payloadHex + "\", " +
                "\"" + F_PORT + "\": \"" + Integer.toString(this.targetPorts) + "\", " +
                "\"" + SECURITY_PARAMS + "\": {" +
                "\"" + AS_ID + "\":  \"" + this.securityAsId + "\", " +
                "\"" + CREATION_TIME + "\": \"" + creationTime + "\", " +
                "\"" + AS_KEY + "\": \"" + this.securityAsKey + "\"" +
                "}" +
                "}";
        getResult(url, HttpMethod.POST, body, true, downlink, msg);
    }

    private ResponseEntity<String> getResult(String url, HttpMethod method, String body, boolean token, DownlinkData downlink, TbMsg msg) {
        ResponseEntity<String> result = sendRestHttp(url, method, body, token, msg);
        if (result != null) {//            Access
            if (result.getStatusCode().is2xxSuccessful()) {
                log.info("[{}] ResponseEntity is2xxSuccessful", result);
                reportDownlinkOk(context, downlink);
            } else {    // Error
                log.info("[{}] ResponseEntity is!2xxSuccessful", result);
                reportDownlinkError(context, msg, result.getStatusCode().toString(), new RuntimeException("ResponseEntity is!2xxSuccessful..."));
            }
        } else {//            Error not token
            log.info("[{}] ResponseEntity Error not token", result);
            reportDownlinkError(context, msg, ERROR_STATUS, new RuntimeException("Error not token, " + HttpStatus.UNAUTHORIZED.toString()));
        }
        return result;
    }

    private ResponseEntity<String> sendRestHttp(String url, HttpMethod method, String body, boolean token, TbMsg msg) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders httpHeaders = createRestHeaders(token);
            if (httpHeaders != null) {
                HttpEntity<String> request = new HttpEntity<String>(body, httpHeaders);
                return restTemplate.exchange(url, method, request, String.class);
            } else {
                log.warn("Failed to process sendRestHttp message, httpHeaders == null", "");
                reportDownlinkError(context, msg, ERROR_STATUS, new RuntimeException("Bad request, httpHeaders == null, " + HttpStatus.UNAUTHORIZED.toString()));
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to process sendRestHttp message, Bad request.", e);
            reportDownlinkError(context, msg, ERROR_STATUS, new RuntimeException("Bad request, " + HttpStatus.UNAUTHORIZED.toString()));
            return null;
        }
    }

    private HttpHeaders createRestHeaders(boolean token) {
        if (token && (this.accessToken == null || this.accessToken.isEmpty())) {
            return null;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (token) {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(accessToken);

        } else {
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        return httpHeaders;
    }

    private boolean checkSecurity(ThingParkIntegrationMsg msg) throws Exception {
        ThingParkRequestParameters params = msg.getParams();
        if (params == null) {
            log.trace("Expected params: {}, actual: {}", "not null", params);
            return false;
        }

        // If securityAsId is not configured, treat as security disabled
        if (securityAsId == null) {
            return true;
        }

        // Security is configured, validate the request
        if (params.getAsId() == null || !securityAsId.equalsIgnoreCase(params.getAsId())) {
            log.trace("Expected securityAsId: {}, actual: {}", securityAsId, params.getAsId());
            return false;
        }

        if (params.getToken() == null || params.getToken().isEmpty()) {
            log.trace("Expected Time: {}, actual: {}", "not null and not empty", params.getToken());
            return false;
        }

        if (params.getLrnDevEui() == null || params.getLrnDevEui().isEmpty()) {
            log.trace("Expected LrnDevEui: {}, actual: {}", "not null and not empty", params.getLrnDevEui());
            return false;
        }

        if (params.getLrnFPort() == null || params.getLrnFPort().isEmpty()) {
            log.trace("Expected LrnFPort: {}, actual: {}", "not null and not empty", params.getLrnFPort());
            return false;
        }

        if (params.getTime() == null || params.getTime().isEmpty()) {
            log.trace("Expected Time: {}, actual: {}", "not null and not empty", params.getTime());
            return false;
        }


        if (!msg.getMsg().has("DevEUI_uplink") && !msg.getMsg().has(devEUiSent)) {
            log.trace("Expected DevEUI_uplink/sent: {}, actual: {}", true, false);
            return false;
        }

        if (msg.getMsg().has("DevEUI_uplink") && !msg.getMsg().get("DevEUI_uplink").has("DevEUI")) {
            log.trace("Expected DevEUI: {}, actual: {}", true, false);
            return false;
        }

        if (msg.getMsg().has(devEUiSent) && !msg.getMsg().get(devEUiSent).has("DevEUI")) {
            log.trace("Expected DevEUI/Sent: {}, actual: {}", true, false);
            return false;
        }

        if (msg.getMsg().has("DevEUI_uplink") && !msg.getMsg().get("DevEUI_uplink").has("FPort")) {
            log.trace("Expected FPort: {}, actual: {}", true, false);
            return false;
        }

        if (msg.getMsg().has(devEUiSent) && !msg.getMsg().get(devEUiSent).has("FPort")) {
            log.trace("Expected FPort/Sent: {}, actual: {}", true, false);
            return false;
        }

        log.trace("Validating request using following raw token: {}", true);
        return true;
    }

}
