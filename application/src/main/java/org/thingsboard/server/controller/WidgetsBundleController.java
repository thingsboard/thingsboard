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
package org.thingsboard.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetsBundleController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetsBundle/{widgetsBundleId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetsBundle getWidgetsBundleById(@PathVariable("widgetsBundleId") String strWidgetsBundleId) throws ThingsboardException {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        try {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
            return checkWidgetsBundleId(widgetsBundleId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetsBundle", method = RequestMethod.POST)
    @ResponseBody
    public WidgetsBundle saveWidgetsBundle(@RequestBody WidgetsBundle widgetsBundle) throws ThingsboardException {
        try {
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
            } else {
                widgetsBundle.setTenantId(getCurrentUser().getTenantId());
            }

            checkEntity(widgetsBundle.getId(), widgetsBundle, Resource.WIDGETS_BUNDLE);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

            sendNotificationMsgToEdgeService(getTenantId(), savedWidgetsBundle.getId(), EntityType.WIDGETS_BUNDLE,
                    widgetsBundle.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

            return checkNotNull(savedWidgetsBundle);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetsBundle/{widgetsBundleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetsBundle(@PathVariable("widgetsBundleId") String strWidgetsBundleId) throws ThingsboardException {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        try {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
            checkWidgetsBundleId(widgetsBundleId, Operation.DELETE);
            widgetsBundleService.deleteWidgetsBundle(getTenantId(), widgetsBundleId);

            sendNotificationMsgToEdgeService(getTenantId(), widgetsBundleId, EntityType.WIDGETS_BUNDLE, EdgeEventActionType.DELETED);

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetsBundles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<WidgetsBundle> getWidgetsBundles(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(widgetsBundleService.findSystemWidgetsBundlesByPageLink(getTenantId(), pageLink));
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                return checkNotNull(widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetsBundles", method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetsBundle> getWidgetsBundles() throws ThingsboardException {
        try {
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(widgetsBundleService.findSystemWidgetsBundles(getTenantId()));
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                return checkNotNull(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
