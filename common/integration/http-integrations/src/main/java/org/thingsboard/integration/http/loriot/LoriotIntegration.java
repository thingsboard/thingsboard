// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LoriotIntegration extends BasicHttpIntegration<JsonHttpIntegrationMsg> {
    private static final String EUI = "EUI";
    private static final String PORT = "port";
    private static final String DATA = "data";
    private static final String APP_ID = "appid";
    private static final String CONFIRMED = "confirmed";
    private static final String IS_HEX_ENCODED = "isHexEncoded";

    private LoriotConfiguration loriotConfiguration;
    private RestTemplate httpClient;
    private WebClient webClient;
    private String baseUrl;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loriotConfiguration = JacksonUtil.fromString(JacksonUtil.toString(configuration.getConfiguration()), LoriotConfiguration.class);

        if (loriotConfiguration.isCreateLoriotOutput() || loriotConfiguration.isSendDownlink()) {
            String domain = loriotConfiguration.getDomain();

            if (domain == null) {
                domain = "loriot.io";
            }

            baseUrl = String.format("https://%s.%s/", loriotConfiguration.getServer(), domain);

            initRestClient();
            if (loriotConfiguration.isCreateLoriotOutput()) {
                loriotConfiguration.getCredentials().setInterceptor(httpClient, baseUrl);
                createApplicationOutputIfNotExist();
            }
            if (loriotConfiguration.isSendDownlink()) {
                HttpClient httpClient = HttpClient.create()
                        .runOn(context.getEventLoopGroup());
                SslContext sslContext = SslContextBuilder.forClient().build();
                httpClient.secure(t -> t.sslContext(sslContext));

                this.webClient = WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
            }
        }
    }

    private void initRestClient() {
        httpClient = new RestTemplate();
        httpClient.setErrorHandler(new DefaultResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    private void createApplicationOutputIfNotExist() {
        if (!isOutputCreated()) {
            ObjectNode newOutput = JacksonUtil.newObjectNode();
            newOutput.put("output", "httppush");
            ObjectNode outputSetup = JacksonUtil.newObjectNode();
            outputSetup.put("url", loriotConfiguration.getHttpEndpoint());
            if (loriotConfiguration.isEnableSecurity() && loriotConfiguration.getHeadersFilter() != null && loriotConfiguration.getHeadersFilter().size() > 0) {
                outputSetup.put("auth", loriotConfiguration.getHeadersFilter().get("Authorization"));
            }
            newOutput.set("osetup", outputSetup);

            ResponseEntity<JsonNode> response =
                    httpClient.postForEntity(baseUrl + "1/nwk/app/" + loriotConfiguration.getAppId() + "/outputs/httppush", newOutput, JsonNode.class);
            HttpStatus responseStatus = (HttpStatus) response.getStatusCode();
            JsonNode error = response.getBody() != null ? response.getBody().get("error") : null;

            if (responseStatus.equals(HttpStatus.OK) || responseStatus.equals(HttpStatus.NO_CONTENT)) {
                // OK
            } else if (responseStatus.equals(HttpStatus.BAD_REQUEST)) {
                if (error == null) {
                    throw new RuntimeException("Failed to start Loriot integration: Unknown ERROR");
                } else if ("OUTPUT_SETUP_APPEND_ERROR".equals(error.asText())) {
                    //OK, output already exist
                } else if ("APP_NOT_FOUND".equals(error.asText())) {
                    throw new RuntimeException("Failed to start Loriot integration: Maximum Application Output Limit Reached.");
                } else {
                    throw new RuntimeException("Failed to start Loriot integration: " + error.asText());
                }
            } else {
                throw new RuntimeException("Failed to start Loriot integration: " + (error != null ? error.asText() : responseStatus.name()));
            }
        }
    }

    private boolean isOutputCreated() {
        ResponseEntity<JsonNode> outputsResponse =
                httpClient.getForEntity(baseUrl + "1/nwk/app/" + loriotConfiguration.getAppId(), JsonNode.class);

        if (outputsResponse.getStatusCode().equals(HttpStatus.OK)) {
            ArrayNode outputs = (ArrayNode) outputsResponse.getBody().get("outputs");
            for (JsonNode output : outputs) {
                if (output.get("output").asText().equals("httppush") && output.get("osetup").get("url").asText().equals(loriotConfiguration.getHttpEndpoint())) {
                    return true;
                }
            }
        } else {
            throw new RuntimeException("Failed to start Lorion integration: Failed to get application data, HTTP Status: " + outputsResponse.getStatusCode());
        }
        return false;
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null && loriotConfiguration.isSendDownlink()) {
            processDownLinkMsg(context, msg);
        }
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
                    String eui = metadata.get(EUI);
                    if (StringUtils.isEmpty(eui)) {
                        throw new RuntimeException("Device EUI is missing in the downlink metadata!");
                    }
                    if (eui.length() != 16) {
                        throw new RuntimeException("Device EUI, 16 hex digits (without dashes)!");
                    }

                    String portStr = metadata.get(PORT);
                    if (StringUtils.isEmpty(portStr)) {
                        throw new RuntimeException("Port is missing in the downlink metadata!");
                    }

                    int port = Integer.parseInt(portStr);
                    if (port < 1 || port > 223) {
                        throw new RuntimeException("Port should be on the range from 1 to 223!");
                    }

                    String data = new String(downlink.getData(), StandardCharsets.UTF_8);

                    ObjectNode payload = JacksonUtil.newObjectNode();
                    // must always have the value 'tx'
                    payload.put("cmd", "tx");
                    payload.put(EUI, eui);
                    payload.put(PORT, port);
                    payload.put(APP_ID, loriotConfiguration.getAppId());

                    String isHexEncodedStr = metadata.get(IS_HEX_ENCODED);
                    if (StringUtils.isNotEmpty(isHexEncodedStr) && Boolean.parseBoolean(isHexEncodedStr)) {
                        payload.put(DATA, data);
                    } else {
                        payload.put(DATA, toHex(data));
                    }

                    if (metadata.containsKey(CONFIRMED)) {
                        payload.put(CONFIRMED, Boolean.parseBoolean(metadata.get(CONFIRMED)));
                    }

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(loriotConfiguration.getToken());

                    Mono<ResponseEntity<String>> responseMono = webClient.post()
                            .uri(loriotConfiguration.getLoriotDownlinkUrl())
                            .headers(h -> h.addAll(headers))
                            .bodyValue(payload)
                            .retrieve()
                            .toEntity(String.class);

                    responseMono.subscribe(
                            voidResponseEntity -> {
                                HttpStatusCode statusCode = voidResponseEntity.getStatusCode();
                                if (statusCode.is2xxSuccessful()) {
                                    reportDownlinkOk(context, downlink);
                                } else {
                                    reportDownlinkError(context, msg, voidResponseEntity.getBody(), new RuntimeException());
                                }
                            },
                            throwable -> {
                                if (throwable instanceof UnknownHttpStatusCodeException) {
                                    reportDownlinkError(context, msg, "ERROR", new Exception(((UnknownHttpStatusCodeException) throwable).getResponseBodyAsString()));
                                } else {
                                    reportDownlinkError(context, msg, "ERROR", new Exception(throwable));
                                }
                            }
                    );

                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
            reportDownlinkError(context, msg, status, exception);
        }
    }

    public String toHex(String data) {
        return Hex.toHexString(data.getBytes());
    }
}
