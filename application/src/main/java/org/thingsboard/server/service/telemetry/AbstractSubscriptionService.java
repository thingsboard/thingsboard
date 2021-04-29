/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Slf4j
public abstract class AbstractSubscriptionService extends TbApplicationEventListener<PartitionChangeEvent>{

    protected final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    protected final TbClusterService clusterService;
    protected final PartitionService partitionService;
    protected Optional<SubscriptionManagerService> subscriptionManagerService;

    protected ExecutorService wsCallBackExecutor;

    public AbstractSubscriptionService(TbClusterService clusterService,
                                       PartitionService partitionService) {
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Autowired(required = false)
    public void setSubscriptionManagerService(Optional<SubscriptionManagerService> subscriptionManagerService) {
        this.subscriptionManagerService = subscriptionManagerService;
    }

    abstract String getExecutorPrefix();

    @PostConstruct
    public void initExecutor() {
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getExecutorPrefix() + "-service-ws-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
        }
    }

    protected <T> void addWsCallback(ListenableFuture<T> saveFuture, Consumer<T> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.accept(null);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, wsCallBackExecutor);
    }
}
