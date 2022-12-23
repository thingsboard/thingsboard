/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationTemplateController extends BaseController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        checkEntity(notificationTemplate.getId(), notificationTemplate, Resource.NOTIFICATION_TEMPLATE);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @DeleteMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTemplate(@PathVariable UUID id) throws Exception {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        NotificationTemplate notificationTemplate = checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::deleteNotificationTemplateById);
    }

}
