// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.remote;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.controller.AbstractIntegrationControllerApi;
import org.thingsboard.integration.service.RemoteIntegrationManagerService;
import org.thingsboard.server.queue.util.TbIntegrationComponent;

import java.util.concurrent.Executor;

@TbIntegrationComponent
@Component
@Slf4j
public class RemoteIntegrationControllerApi extends AbstractIntegrationControllerApi {

    private ListeningExecutorService service;

    @Value("${executors.thread_pool_size}")
    private int executorThreadPoolSize;

    @Autowired
    private RemoteIntegrationManagerService managerService;

    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(executorThreadPoolSize, getClass()));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    protected ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey) {
        return Futures.immediateFuture(managerService.getIntegration());
    }

    @Override
    protected Executor getCallbackExecutor() {
        return service;
    }
}
