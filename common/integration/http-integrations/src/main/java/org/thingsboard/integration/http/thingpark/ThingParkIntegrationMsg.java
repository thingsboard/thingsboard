// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
public class ThingParkIntegrationMsg extends JsonHttpIntegrationMsg {

    @Getter
    private final ThingParkRequestParameters params;

    public ThingParkIntegrationMsg(Map<String, String> requestHeaders, JsonNode msg, ThingParkRequestParameters params,
                                   DeferredResult<ResponseEntity> callback) {
        super(requestHeaders, msg, callback);
        this.params = params;
    }

}
