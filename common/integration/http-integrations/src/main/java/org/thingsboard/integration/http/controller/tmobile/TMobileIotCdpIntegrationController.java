// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.controller.tmobile;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.controller.BaseIntegrationController;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.util.TbIntegrationExecutorOrIntegrationComponent;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/tmobile_iot_cdp")
@Slf4j
@TbIntegrationExecutorOrIntegrationComponent
public class TMobileIotCdpIntegrationController extends BaseIntegrationController {

    @Operation(description = "Process request from T Mobile IoT integration", hidden = true)
    @RequestMapping(value = "/{routingKey}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void processCheck(
            @PathVariable("routingKey") String routingKey,
            @RequestHeader(required = false) Map<String, String> requestHeaders
    ) {
        log.debug("[{}] Received validation request: {}", routingKey, requestHeaders);
    }

    @Operation(description = "Process request from T Mobile IoT integration", hidden = true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    @RequestMapping(value = "/{routingKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders
    ) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        api.process(IntegrationType.TMOBILE_IOT_CDP, routingKey, result, new JsonHttpIntegrationMsg(requestHeaders, msg, result));

        return result;
    }
}
