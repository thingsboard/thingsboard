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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.AlarmId;
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

    @Lazy
    final TbAlarmService alarmService;

    ListeningExecutorService executor;

    AtomicInteger queueSize = new AtomicInteger();

    @PostConstruct
    public void init() {
         executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper")));
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public ListenableFuture<List<AlarmId>> unassignDeletedUserAlarms(User user) {
        log.debug("[{}][{}] unassignDeletedUserAlarms submitting, pending queue size: {} ", user.getTenantId().getId(), user.getId().getId(), queueSize.get());
        queueSize.incrementAndGet();
        ListenableFuture<List<AlarmId>> future = executor.submit(() -> alarmService.unassignDeletedUserAlarms(user));
        future.addListener(() -> {
            queueSize.decrementAndGet();
            log.debug("[{}][{}] unassignDeletedUserAlarms finished, pending queue size: {} ", user.getTenantId().getId(), user.getId().getId(), queueSize.get());
        }, MoreExecutors.directExecutor());
        return future;
    }

}
