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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class NotificationRuleController extends BaseController {
    // todo: logEntityAction
    private final NotificationRuleService notificationRuleService;

    @PostMapping("/notification/rule")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public NotificationRule saveNotificationRule(@RequestBody NotificationRule notificationRule,
                                                 @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
//        accessControlService.checkPermission(user, Resource.NOTIFICATION_RULE, notificationRule.getId() == null ? Operation.CREATE : Operation.WRITE, notificationRule.getId(), );
        return notificationRuleService.saveNotificationRule(user.getTenantId(), notificationRule);
    }

    @GetMapping("/notification/rule/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public NotificationRule getNotificationRuleById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        NotificationRuleId notificationRuleId = new NotificationRuleId(id);
        NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(user.getTenantId(), notificationRuleId);
        accessControlService.checkPermission(user, Resource.NOTIFICATION_RULE, Operation.READ, notificationRuleId, notificationRule);
        return notificationRule;
    }

    @GetMapping("/notification/rules")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public PageData<NotificationRule> getNotificationRules(@RequestParam int pageSize,
                                                           @RequestParam int page,
                                                           @RequestParam(required = false) String textSearch,
                                                           @RequestParam(required = false) String sortProperty,
                                                           @RequestParam(required = false) String sortOrder,
                                                           @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationRuleService.findNotificationRulesByTenantId(user.getTenantId(), pageLink);
    }

}
