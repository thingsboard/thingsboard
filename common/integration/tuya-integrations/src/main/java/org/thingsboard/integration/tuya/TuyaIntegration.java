// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.HmacSHA256Util;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.tuya.mq.EncryptionMethod;
import org.thingsboard.integration.tuya.mq.MessageVO;
import org.thingsboard.integration.tuya.mq.MqConsumer;
import org.thingsboard.integration.tuya.mq.TuyaMessageUtil;
import org.thingsboard.integration.tuya.util.ServiceRPC;
import org.thingsboard.integration.tuya.util.TuyaToken;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.integration.tuya.TuyaIntegrationTask.CHECK_CONNECTION;
import static org.thingsboard.integration.tuya.TuyaIntegrationTask.CONNECT;
import static org.thingsboard.integration.tuya.TuyaIntegrationTask.DISCONNECT;
import static org.thingsboard.integration.tuya.TuyaIntegrationTask.RECONNECT;

@Slf4j
public class TuyaIntegration extends AbstractIntegration<TuyaIntegrationMsg> {

    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private final static int TOKEN_GRANT_TYPE = 1;
    private final static String GET_TOKEN_URL_PATH = "/v1.0/token";
    private final static String GET_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    private final static String POST_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";

    private final static int DEFAULT_DELAY = 60;

    private final RestTemplate httpClient = new RestTemplate();
    private TuyaToken accessToken;
    private ExecutorService executor;
    private TuyaIntegrationConfiguration tuyaIntegrationConfiguration;
    private MqConsumer mqConsumer;
    private volatile ScheduledFuture<?> nextPollFuture;
    private final BlockingQueue<TuyaIntegrationTask> taskQueue = new LinkedBlockingQueue<>();
    private final Lock taskLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        accessToken = null;
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        tuyaIntegrationConfiguration = getClientConfiguration(configuration, TuyaIntegrationConfiguration.class);
        mqConsumer = createMqConsumer(tuyaIntegrationConfiguration.getAccessId(), tuyaIntegrationConfiguration.getAccessKey());
        submit(CONNECT);
    }

    private void submitPoll() {
        context.getExecutorService().execute(this::pollTask);
    }

    private void pollTask() {
        taskLock.lock();
        try {
            TuyaIntegrationTask task = taskQueue.poll();
            if (task != null) {
                deduplicate(task);
                log.debug("[{}] Going to process task: {}", getConfigurationId(), task);
                processTask(task);
            }
        } catch (Exception e) {
            log.warn("[{}] Unhandled error during task processing: ", getConfigurationId(), e);
        } finally {
            taskLock.unlock();
        }
    }

    private void processTask(TuyaIntegrationTask task) {
        switch (task) {
            case CONNECT:
                doConnect();
                break;
            case DISCONNECT:
                doDisconnect();
                break;
            case RECONNECT:
                doReconnect();
                break;
            case CHECK_CONNECTION:
                doCheckConnectionToService();
                break;
        }
    }

    private void doConnect() {
        try {
            mqConsumer.connect();
            submit(CHECK_CONNECTION, DEFAULT_DELAY);
            resultHandler("CONNECT", "", null);
        } catch (Exception e) {
            resultHandler("CONNECT", e.getMessage(), e);
            submit(RECONNECT, DEFAULT_DELAY);
        }
    }

    private void doDisconnect() {
        try {
            mqConsumer.stopClient();
        } catch (PulsarClientException e) {
            log.error("[{}] Cannot disconnect client correctly!", getConfigurationId(), e);
        }
    }

    private void doReconnect() {
        submit(DISCONNECT);
        submit(CONNECT, DEFAULT_DELAY);
    }

    private void doCheckConnectionToService() {
        if (mqConsumer.checkConnection()) {
            submit(CHECK_CONNECTION, DEFAULT_DELAY);
            return;
        }
        submit(DISCONNECT);
        submit(CONNECT);
    }

    private void submit(TuyaIntegrationTask task) {
        submit(task, 0);
    }

    private void submit(TuyaIntegrationTask task, int delayInSec) {
        if (delayInSec > 0) {
            log.debug("[{}] Adding task to queue: {} with delay {}", getConfigurationId(), task, delayInSec);
        } else {
            log.debug("[{}] Adding task to queue: {}", getConfigurationId(), task);
        }
        if (nextPollFuture != null) {
            nextPollFuture.cancel(true);
        }
        if (DISCONNECT.equals(task)) {
            taskQueue.removeIf(Objects::nonNull);
        }
        taskQueue.add(task);
        log.debug("[{}] queue size: {}", getConfigurationId(), taskQueue.size());
        if (delayInSec > 0) {
            nextPollFuture = context.getScheduledExecutorService().schedule(this::submitPoll, delayInSec, TimeUnit.SECONDS);
        } else {
            submitPoll();
        }
    }

    private void deduplicate(TuyaIntegrationTask task) {
        while (true) {
            TuyaIntegrationTask next = taskQueue.peek();
            if (task.equals(next) || (RECONNECT.equals(task) && (DISCONNECT.equals(next) || (CONNECT.equals(next))))) {
                log.debug("[{}] Remove duplicated task from queue: {}", getConfigurationId(), next);
                taskQueue.poll();
            } else {
                break;
            }
        }
    }

    @Override
    public void destroy() {
        List<Runnable> runnables = executor.shutdownNow();
        log.debug("Stopped executor service, list of returned runnables: {}", runnables);
        submit(DISCONNECT);
    }

    @Override
    public void process(TuyaIntegrationMsg msg) {
        if (this.configuration.isEnabled()) {
            Exception exception = null;
            try {
                doProcess(msg);
                integrationStatistics.incMessagesProcessed();
            } catch (Exception e) {
                log.debug("[{}] Failed to apply data converter function: {}", getConfigurationId(), e.getMessage(), e);
                exception = e;
                integrationStatistics.incErrorsOccurred();
            }
            persistDebug(this.context, "Uplink", ContentType.JSON, msg.toString(), exception == null ? "OK" : "ERROR", exception);
        }
    }

    private void doProcess(TuyaIntegrationMsg msg) throws Exception {
        byte[] data = JacksonUtil.writeValueAsBytes(msg.getJson());
        Map<String, String> metadataMap = msg.getDeviceMetadata();
        List<UplinkData> uplinkDataList = this.convertToUplinkDataList(this.context, data, new UplinkMetaData<>(ContentType.JSON, metadataMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                UplinkData uplinkDataResult = UplinkData.builder().deviceName(uplinkData.getDeviceName())
                        .deviceType(uplinkData.getDeviceType()).telemetry(uplinkData.getTelemetry())
                        .attributesUpdate(uplinkData.getAttributesUpdate()).customerName(uplinkData.getCustomerName())
                        .build();
                this.processUplinkDataBlocking(this.context, uplinkDataResult);
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            Exception exception = null;
            try {
                if (doProcessDownLinkMsg(msg)) {
                    integrationStatistics.incMessagesProcessed();
                }
            } catch (Exception e) {
                exception = e;
            }
            resultHandler("Downlink", msg.getData(), exception);
        }
    }

    private void resultHandler(String type, String msg, Exception exception) {
        String status = exception == null ? "SUCCESS" : "FAILURE";
        persistDebug(context, type, ContentType.JSON, msg, status, exception);
    }

    private MqConsumer createMqConsumer(String accessId, String accessKey) {
        return MqConsumer.builder()
                .serviceUrl(tuyaIntegrationConfiguration.getRegion().getMessagingServerUrl())
                .accessId(accessId)
                .accessKey(accessKey)
                .env(tuyaIntegrationConfiguration.getEnv())
                .messageListener((incomingData) -> {
                    MessageVO vo = JacksonUtil.fromBytes(incomingData.getData(), MessageVO.class);
                    Map<String, String> props = incomingData.getProperties();
                    EncryptionMethod em = Optional.ofNullable(props.get("em"))
                            .map(EncryptionMethod::forCode)
                            .orElse(EncryptionMethod.AES_ECB);
                    Map<String, String> metadata = new HashMap<>(metadataTemplate.getKvMap());
                    if (vo != null) {
                        String decryptedData = "";
                        try {
                            metadata.put("protocol", vo.getProtocol().toString());
                            metadata.put("pv", vo.getPv());
                            metadata.put("sign", vo.getSign());
                            metadata.put("t", vo.getT().toString());
                            metadata.put("topic", incomingData.getTopicName());
                            decryptedData = TuyaMessageUtil.decrypt(vo.getData(), accessKey.substring(8, 24), em);
                            JsonNode dataNode = JacksonUtil.fromString(decryptedData, JsonNode.class);
                            TuyaIntegrationMsg msg = new TuyaIntegrationMsg(dataNode, metadata);
                            this.process(msg);
                        } catch (Exception e) {
                            resultHandler("Uplink", decryptedData, e);
                        }
                    }
                })
                .resultHandler((this::resultHandler))
                .build();
    }

    private boolean doProcessDownLinkMsg(TbMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.putAll(msg.getMetaData().getData());
        List<DownlinkData> convertedDownlinkData = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        if (convertedDownlinkData == null || convertedDownlinkData.isEmpty()) {
            throw new RuntimeException("Data should contain commands array or code and value of the command");
        }
        Exception error = null;
        ArrayNode result = JacksonUtil.newArrayNode();
        for (DownlinkData downlinkData : convertedDownlinkData) {
            try {
                String deviceId = getDeviceIdentifier(downlinkData.getMetadata());
                ObjectNode commandsNode = JacksonUtil.newObjectNode();
                ArrayNode arrayNode = JacksonUtil.newArrayNode();
                JsonNode data = JacksonUtil.fromBytes(downlinkData.getData());
                ServiceRPC serviceRPC = extractServiceRPC(data);
                if (!hasValidAccessToken()) {
                    accessToken = getToken();
                }

                if (accessToken == null) {
                    throw new RuntimeException("Cannot obtain access token from the server: " + tuyaIntegrationConfiguration.getRegion().getApiServerUrl());
                }
                if (serviceRPC == null) {
                    if (data instanceof ArrayNode) {
                        arrayNode = (ArrayNode) data;
                    } else {
                        if (data.has("code")) {
                            arrayNode.add(data);
                        } else {
                            throw new RuntimeException("Downlink message format is not correct, downlink message should contain one of the following: code field, service RPC method, commands array!");
                        }
                    }
                    commandsNode.set("commands", arrayNode);

                    String path = String.format(POST_COMMANDS_URL_PATH, deviceId);
                    result.add(sendPostRequest(path, commandsNode));
                } else {
                    String path = serviceRPC.path;
                    if (serviceRPC.requiresParameter) {
                        path = createServiceRPCPathWithParameter(path, data);
                    } else {
                        path = String.format(path, deviceId);
                    }
                    ObjectNode responseBody = sendGetRequest(path);
                    Map<String, String> metadata = new HashMap<>(metadataTemplate.getKvMap());
                    metadata.put("msgType", "DownlinkResponse");
                    metadata.put("deviceId", deviceId);
                    metadata.put("t", responseBody.get("t").asText());
                    JsonNode dataNode = responseBody.get("result");
                    TuyaIntegrationMsg tuyaIntegrationMsg = new TuyaIntegrationMsg(dataNode, metadata);
                    this.process(tuyaIntegrationMsg);
                }
            } catch (Exception e) {
                error = e;
            }
        }
        if (error != null) {
            throw error;
        }
        return true;
    }

    private ServiceRPC extractServiceRPC(JsonNode data) {
        if (data.has("method")) {
            String method = data.get("method").asText();
            for (ServiceRPC serviceRPC : ServiceRPC.values()) {
                if (serviceRPC.method.equalsIgnoreCase(method)) return serviceRPC;
            }
        }
        return null;
    }

    private String createServiceRPCPathWithParameter(String path, JsonNode data) {
        if (!data.has("parameter")) {
            throw new RuntimeException(String.format("[%s] Required \"parameter\" field not found!", getConfigurationId()));
        }
        return String.format(path, data.get("parameter").asText());
    }

    private TuyaToken getToken() throws Exception {
        String path = "";
        if (hasValidAccessToken()) {
            path = String.format(GET_REFRESH_TOKEN_URL_PATH, accessToken.getRefreshToken());
        } else {
            Map<String, Object> queries = new HashMap<>();
            queries.put("grant_type", TOKEN_GRANT_TYPE);
            path = GET_TOKEN_URL_PATH + "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                    .collect(Collectors.joining("&"));
        }
        RequestEntity<Object> requestEntity = createGetRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (validateResponse(responseEntity)) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }


    private ObjectNode sendPostRequest(String path, ObjectNode commandsNode) throws Exception {
        RequestEntity<Object> requestEntity = createPostRequest(path, commandsNode);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        ObjectNode result = responseEntity.getBody();
        log.debug("[{}] Received response: [{}]", getConfigurationId(), result);
        return result;
    }

    private ObjectNode sendGetRequest(String path) throws Exception {
        RequestEntity<Object> requestEntity = createGetRequest(path, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        ObjectNode result = responseEntity.getBody();
        log.debug("[{}] Received response: [{}]", getConfigurationId(), result);
        return result;
    }

    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity) {
        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(),
                Objects.requireNonNull(requestEntity.getMethod()),
                requestEntity,
                ObjectNode.class);
        if (!validateResponse(responseEntity)) {
            throw new RuntimeException(String.format("[%s] No response for RPC request! Reason code from Tuya Cloud: %s",
                    getConfigurationId(),
                    responseEntity.getStatusCode()));
        }
        if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
            return responseEntity;
        } else {
            throw new RuntimeException(String.format("[%s] Tuya integration response error code: [%s], msg: [%s]",
                    getConfigurationId(),
                    responseEntity.getBody().get("code").asInt(),
                    responseEntity.getBody().get("msg").asText()));
        }
    }

    private RequestEntity<Object> createGetRequest(String path, boolean getToken) throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        String strToSign;
        if (getToken) {
            strToSign = tuyaIntegrationConfiguration.getAccessId() + ts + formatStringToSign(path, getBodyHash(null), httpMethod);
        } else {
            httpHeaders.add("access_token", accessToken.getAccessToken());
            strToSign = tuyaIntegrationConfiguration.getAccessId() + accessToken.getAccessToken() + ts + formatStringToSign(path, getBodyHash(null), httpMethod);
        }
        String signedStr = sign(strToSign, tuyaIntegrationConfiguration.getAccessKey());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(tuyaIntegrationConfiguration.getRegion().getApiServerUrl() + path);
        return new RequestEntity<>(httpHeaders, httpMethod, uri);
    }

    private RequestEntity<Object> createPostRequest(String path, ObjectNode body) throws Exception {
        HttpMethod httpMethod = HttpMethod.POST;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        httpHeaders.add("access_token", accessToken.getAccessToken());
        String strToSign = tuyaIntegrationConfiguration.getAccessId() + accessToken.getAccessToken() +
                ts + formatStringToSign(path, getBodyHash(body.toString()), httpMethod);
        String signedStr = sign(strToSign, tuyaIntegrationConfiguration.getAccessKey());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(tuyaIntegrationConfiguration.getRegion().getApiServerUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private String formatStringToSign(String path, String bodyHash, HttpMethod httpMethod) {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }

    private String getBodyHash(String body) throws Exception {
        if (StringUtils.isBlank(body)) {
            return EMPTY_HASH;
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(messageDigest.digest());
        }
    }

    private String sign(String content, String secret) throws Exception {
        byte[] rawHmac = HmacSHA256Util.sign(content, secret.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(rawHmac).toUpperCase();
    }

    private HttpHeaders createHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("client_id", tuyaIntegrationConfiguration.getAccessId());
        httpHeaders.add("t", ts);
        httpHeaders.add("sign_method", "HMAC-SHA256");
        httpHeaders.add("nonce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private String getDeviceIdentifier(Map<String, String> metadata) {
        String deviceId = null;
        if (metadata.containsKey(DataConstants.DEVICE_ID)) {
            deviceId = metadata.get(DataConstants.DEVICE_ID);
        } else if (metadata.containsKey(DataConstants.DEVICE_NAME)) {
            deviceId = metadata.get(DataConstants.DEVICE_NAME);
        }
        if (deviceId == null) {
            throw new RuntimeException("Device identifier not found in metadata! Expected deviceId or deviceName parameters");
        }
        return deviceId;
    }

    private boolean hasValidAccessToken() {
        return accessToken != null && accessToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }

    private boolean validateResponse(ResponseEntity<ObjectNode> responseEntity) {
        return responseEntity != null
                && HttpStatus.OK.equals(responseEntity.getStatusCode())
                && responseEntity.getBody() != null;
    }

    private String getConfigurationId() {
        return this.configuration.getName();
    }

}
