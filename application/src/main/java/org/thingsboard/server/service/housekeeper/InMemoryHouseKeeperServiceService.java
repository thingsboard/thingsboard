/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.housekeeper;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.housekeeper.HouseKeeperService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryHouseKeeperServiceService implements HouseKeeperService {

    final TbAlarmService alarmService;

    ListeningExecutorService executor;

    AtomicInteger queueSize = new AtomicInteger();
    AtomicInteger totalProcessedCounter = new AtomicInteger();

    @PostConstruct
    public void init() {
        log.debug("Starting HouseKeeper service");
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper")));
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            log.debug("Stopping HouseKeeper service");
            executor.shutdown();
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        log.trace("[{}] DeleteEntityEvent handler: {}", event.getTenantId(), event);
        EntityId entityId = event.getEntityId();
        if (EntityType.USER.equals(entityId.getEntityType())) {
            unassignDeletedUserAlarms(event.getTenantId(), (User) event.getEntity(), event.getTs());
        }
    }

    @Override
    public ListenableFuture<List<AlarmId>> unassignDeletedUserAlarms(TenantId tenantId, User user, long unassignTs) {
        log.debug("[{}][{}] unassignDeletedUserAlarms submitting, pending queue size: {} ", tenantId, user.getId().getId(), queueSize.get());
        queueSize.incrementAndGet();
        ListenableFuture<List<AlarmId>> future = executor.submit(() -> alarmService.unassignDeletedUserAlarms(tenantId, user, unassignTs));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(List<AlarmId> alarmIds) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.debug("[{}][{}] unassignDeletedUserAlarms finished, pending queue size: {}, total processed count: {} ",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get());
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.error("[{}][{}] unassignDeletedUserAlarms failed, pending queue size: {}, total processed count: {}",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get(), throwable);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

}
