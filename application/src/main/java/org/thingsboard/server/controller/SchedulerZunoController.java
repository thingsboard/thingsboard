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
package org.thingsboard.server.controller;


import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.scheduler.SchedulerZunoService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class SchedulerZunoController extends BaseController {

    private final TelemetrySubscriptionService telemetrySubscriptionService;
    private final SchedulerZunoService schedulerZunoService;
    private final AccessValidator accessValidator;

    @PostMapping("/scheduler_zuno")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public String createSchedulerZuno() throws ThingsboardException {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(new LongDataEntry("Timehienxx", System.currentTimeMillis()), System.currentTimeMillis()));
        SecurityUser user = getCurrentUser();
        accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_ATTRIBUTES, EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, "b11e6f70-2390-11f0-8809-2f8f7ccebb3c"), (result, tenantId, entityId) -> {
            telemetrySubscriptionService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(entityId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entries(attributes)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            logAttributesUpdated(user, entityId, AttributeScope.SERVER_SCOPE, attributes, null);
                            result.setResult(new ResponseEntity(HttpStatus.OK));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    })
                    .build());
        });

        return "Scheduler Zuno created successfully";
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_UPDATED, user,
                toException(e), scope, attributes);
    }


}
