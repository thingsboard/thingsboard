// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.data.ContentType;

import java.util.Map;

public class StringHttpIntegrationMsg extends HttpIntegrationMsg<String> {

    public StringHttpIntegrationMsg(Map<String, String> requestHeaders, String msg, DeferredResult<ResponseEntity> callback) {
        super(requestHeaders, msg, callback);
    }

    @Override
    public ContentType getContentType() {
        return ContentType.TEXT;
    }

    @Override
    public byte[] getMsgInBytes() {
        return msg.getBytes();
    }
}
