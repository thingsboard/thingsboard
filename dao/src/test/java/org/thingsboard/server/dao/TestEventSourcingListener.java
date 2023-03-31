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
package org.thingsboard.server.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.device.DeviceCacheEvictEvent;
import org.thingsboard.server.dao.asset.AssetCacheEvictEvent;
import org.thingsboard.server.dao.asset.AssetProfileEvictEvent;
import org.thingsboard.server.dao.device.DeviceCredentialsEvictEvent;
import org.thingsboard.server.dao.device.DeviceProfileEvictEvent;
import org.thingsboard.server.dao.edge.EdgeCacheEvictEvent;
import org.thingsboard.server.dao.entityview.EntityViewEvictEvent;
import org.thingsboard.server.dao.ota.OtaPackageCacheEvictEvent;
import org.thingsboard.server.dao.relation.EntityRelationEvent;
import org.thingsboard.server.dao.tenant.TenantEvictEvent;
import org.thingsboard.server.dao.tenant.TenantProfileEvictEvent;
import org.thingsboard.server.dao.user.UserSettingsEvictEvent;

import javax.annotation.PostConstruct;

/**
 * The purpose of this class is to log or set a checkpoint during debug
 * */

@Component
@Slf4j
public class TestEventSourcingListener {

    @PostConstruct
    public void init() {
        log.debug("EventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(AssetProfileEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(AssetCacheEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeviceCredentialsEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeviceProfileEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeviceCacheEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(EdgeCacheEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(EntityViewEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(OtaPackageCacheEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(EntityRelationEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(TenantProfileEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(TenantEvictEvent event){
        log.debug("event called: {}", event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(UserSettingsEvictEvent event){
        log.debug("event called: {}", event);
    }

}
