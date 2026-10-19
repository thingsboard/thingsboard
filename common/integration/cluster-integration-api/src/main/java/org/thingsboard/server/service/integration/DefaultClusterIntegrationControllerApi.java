// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.controller.AbstractIntegrationControllerApi;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Component
public class DefaultClusterIntegrationControllerApi extends AbstractIntegrationControllerApi {

    private ExecutorService callbackExecutorService;

    @PostConstruct
    public void init() {
        callbackExecutorService = ThingsBoardExecutors.newWorkStealingPool(20, "integration-controller-callback");
    }

    @PreDestroy
    public void destroy() {
        if (callbackExecutorService != null) {
            callbackExecutorService.shutdownNow();
        }
    }

    @Autowired
    private IntegrationManagerService integrationService;


    @Override
    public Executor getCallbackExecutor() {
        return callbackExecutorService;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    protected ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey) {
        return integrationService.getIntegrationByRoutingKey(routingKey);
    }

}
