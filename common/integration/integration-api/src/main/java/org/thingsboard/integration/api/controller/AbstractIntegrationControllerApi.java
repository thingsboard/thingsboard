// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.controller;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationControllerApi;
import org.thingsboard.integration.api.IntegrationHttpMsgProcessor;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.concurrent.Executor;

@Slf4j
public abstract class AbstractIntegrationControllerApi implements IntegrationControllerApi {

    @Override
    public <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg) {
        process(type, routingKey, result, msg, (integration, tmpResult, tmpMsg) -> integration.process(tmpMsg));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg, IntegrationHttpMsgProcessor<T> processor) {
        ListenableFuture<ThingsboardPlatformIntegration> integrationFuture = getIntegrationByRoutingKey(routingKey);

        DonAsynchron.withCallback(integrationFuture, integration -> {
            var theIntegration = (ThingsboardPlatformIntegration<T>)  integration;
            if (checkIntegrationPlatform(result, theIntegration, type)) {
                return;
            }
            processor.process(theIntegration, result, msg);
        }, failure -> {
            log.trace("[{}] Failed to fetch integration by routing key", routingKey, failure);
            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }, getCallbackExecutor());
    }

    @SuppressWarnings({"rawtypes"})
    private static boolean checkIntegrationPlatform(DeferredResult<ResponseEntity> result, ThingsboardPlatformIntegration integration, IntegrationType type) {
        if (integration == null) {
            result.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            return true;
        }
        if (integration.getConfiguration().getType() != type) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return true;
        }
        return false;
    }

    protected abstract Executor getCallbackExecutor();

    protected abstract ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey);

}
