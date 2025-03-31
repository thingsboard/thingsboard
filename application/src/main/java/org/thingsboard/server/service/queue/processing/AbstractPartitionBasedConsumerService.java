/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.processing;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractPartitionBasedConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends AbstractConsumerService<N> {

    private final Lock startupLock = new ReentrantLock();
    private volatile boolean started = false;
    private PartitionChangeEvent lastPartitionChangeEvent;

    public AbstractPartitionBasedConsumerService(ActorSystemContext actorContext,
                                                 TbTenantProfileCache tenantProfileCache,
                                                 TbDeviceProfileCache deviceProfileCache,
                                                 TbAssetProfileCache assetProfileCache,
                                                 CalculatedFieldCache calculatedFieldCache,
                                                 TbApiUsageStateService apiUsageStateService,
                                                 PartitionService partitionService,
                                                 ApplicationEventPublisher eventPublisher,
                                                 JwtSettingsService jwtSettingsService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, calculatedFieldCache, apiUsageStateService, partitionService, eventPublisher, jwtSettingsService);
    }

    @PostConstruct
    public void init() {
        super.init(getPrefix());
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    @Override
    public void afterStartUp() {
        super.afterStartUp();
        onStartUp();
        startupLock.lock();
        try {
            onPartitionChangeEvent(lastPartitionChangeEvent);
            started = true;
        } finally {
            startupLock.unlock();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (!started) {
            startupLock.lock();
            try {
                if (!started) {
                    lastPartitionChangeEvent = event;
                    return;
                }
            } finally {
                startupLock.unlock();
            }
        }
        onPartitionChangeEvent(event);
    }

    protected abstract void onStartUp();

    protected abstract void onPartitionChangeEvent(PartitionChangeEvent event);

    protected abstract String getPrefix();

}
