// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.data.ContentType;

import java.util.Map;

@Data
@AllArgsConstructor
public abstract class HttpIntegrationMsg<T> {

    private final Map<String, String> requestHeaders;
    protected final T msg;
    private final DeferredResult<ResponseEntity> callback;

    public abstract ContentType getContentType();
    public abstract byte[] getMsgInBytes();

}
