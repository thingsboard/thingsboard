// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;

import java.util.Map;

public class JsonHttpIntegrationMsg extends HttpIntegrationMsg<JsonNode> {

    public JsonHttpIntegrationMsg(Map<String, String> requestHeaders, JsonNode msg, DeferredResult<ResponseEntity> callback) {
        super(requestHeaders, msg, callback);
    }

    @Override
    public ContentType getContentType() {
        return ContentType.JSON;
    }

    @Override
    public byte[] getMsgInBytes() {
        return JacksonUtil.writeValueAsBytes(msg);
    }
}
