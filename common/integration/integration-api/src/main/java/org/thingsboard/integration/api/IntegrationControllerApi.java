// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.integration.IntegrationType;

public interface IntegrationControllerApi {

    <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg);

    <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg, IntegrationHttpMsgProcessor<T> processor);

}
