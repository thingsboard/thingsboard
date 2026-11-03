// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.service.security.ValidationCallback;

/**
 * Created by ashvayka on 21.02.17.
 */
public class HttpValidationCallback extends ValidationCallback<DeferredResult<ResponseEntity>> {

    public HttpValidationCallback(DeferredResult<ResponseEntity> response, FutureCallback<DeferredResult<ResponseEntity>> action) {
       super(response, action);
    }

}
