// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public interface IntegrationHttpMsgProcessor<T> {

    void process(ThingsboardPlatformIntegration<T> integration, DeferredResult<ResponseEntity> result, T msg);

}
