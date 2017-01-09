/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WidgetTypeController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetTypeById(@PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            return checkWidgetTypeId(widgetTypeId, false);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetType saveWidgetType(@RequestBody WidgetType widgetType) throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                widgetType.setTenantId(new TenantId(ModelConstants.NULL_UUID));
            } else {
                widgetType.setTenantId(getCurrentUser().getTenantId());
            }
            return checkNotNull(widgetTypeService.saveWidgetType(widgetType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(@PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            checkWidgetTypeId(widgetTypeId, true);
            widgetTypeService.deleteWidgetType(widgetTypeId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypes", params = { "isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = new TenantId(ModelConstants.NULL_UUID);
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = { "isSystem", "bundleAlias", "alias" }, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias,
            @RequestParam String alias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = new TenantId(ModelConstants.NULL_UUID);
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, bundleAlias, alias);
            checkWidgetType(widgetType, false);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
