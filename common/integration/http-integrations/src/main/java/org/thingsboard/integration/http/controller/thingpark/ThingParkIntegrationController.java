// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.controller.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.controller.BaseIntegrationController;
import org.thingsboard.integration.api.util.TbIntegrationExecutorOrIntegrationComponent;
import org.thingsboard.integration.http.thingpark.ThingParkIntegrationMsg;
import org.thingsboard.integration.http.thingpark.ThingParkRequestParameters;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
@Slf4j
@TbIntegrationExecutorOrIntegrationComponent
public class ThingParkIntegrationController extends BaseIntegrationController {

    @Operation(description = "Process request from ThingPark integrations", hidden = true)
    @SuppressWarnings("rawtypes")
    @RequestMapping("/thingpark/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String, String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult(allRequestParams, IntegrationType.THINGPARK, requestHeaders, routingKey, msg);
    }

    @Operation(description = "Process request from ThingPark integrations", hidden = true)
    @SuppressWarnings("rawtypes")
    @RequestMapping("/tpe/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequestTPE(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String, String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult(allRequestParams, IntegrationType.TPE, requestHeaders, routingKey, msg);
    }

    @SuppressWarnings("unchecked")
    private DeferredResult<ResponseEntity> getResult(Map<String, String> allRequestParams, IntegrationType typeIntegration,
                                                     Map<String, String> requestHeaders, String routingKey, JsonNode msg) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        JsonNode jsonNode = JacksonUtil.convertValue(allRequestParams, JsonNode.class);
        String asId = jsonNode.has("AS_ID") ? jsonNode.get("AS_ID").asText() : "false";
        String lrnDevEui = jsonNode.has("LrnDevEui") ? jsonNode.get("LrnDevEui").asText() : "false";
        String lrnFPort = jsonNode.has("LrnFPort") ? jsonNode.get("LrnFPort").asText() : "false";
        String lrnInfos = jsonNode.has("LrnInfos") ? jsonNode.get("LrnInfos").asText() : "false";
        String time = jsonNode.has("Time") ? jsonNode.get("Time").asText() : "false";
        String token = jsonNode.has("Token") ? jsonNode.get("Token").asText() : "false";
        ThingParkRequestParameters params = ThingParkRequestParameters.builder()
                .asId(asId)
                .lrnDevEui(lrnDevEui)
                .lrnFPort(lrnFPort)
                .lrnInfos(lrnInfos)
                .time(time)
                .token(token)
                .build();

        api.process(typeIntegration, routingKey, result, new ThingParkIntegrationMsg(requestHeaders, msg, params, result));

        return result;
    }
}
