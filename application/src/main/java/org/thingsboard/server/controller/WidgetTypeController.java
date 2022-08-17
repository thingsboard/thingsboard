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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_TYPE_ID_PARAM_DESCRIPTION;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetTypeController extends AutoCommitController {

    private static final String WIDGET_TYPE_DESCRIPTION = "Widget Type represents the template for widget creation. Widget Type and Widget are similar to class and object in OOP theory.";
    private static final String WIDGET_TYPE_DETAILS_DESCRIPTION = "Widget Type Details extend Widget Type and add image and description properties. " +
            "Those properties are useful to edit the Widget Type but they are not required for Dashboard rendering. ";
    private static final String WIDGET_TYPE_INFO_DESCRIPTION = "Widget Type Info is a lightweight object that represents Widget Type but does not contain the heavyweight widget descriptor JSON";


    @ApiOperation(value = "Get Widget Type Details (getWidgetTypeById)",
            notes = "Get the Widget Type Details based on the provided Widget Type Id. " + WIDGET_TYPE_DETAILS_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetTypeDetails getWidgetTypeById(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
        return checkWidgetTypeId(widgetTypeId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Widget Type (saveWidgetType)",
            notes = "Create or update the Widget Type. " + WIDGET_TYPE_DESCRIPTION + " " +
                    "When creating the Widget Type, platform generates Widget Type Id as " + UUID_WIKI_LINK +
                    "The newly created Widget Type Id will be present in the response. " +
                    "Specify existing Widget Type id to update the Widget Type. " +
                    "Referencing non-existing Widget Type Id will cause 'Not Found' error." +
                    "\n\nWidget Type alias is unique in the scope of Widget Bundle. " +
                    "Special Tenant Id '13814000-1dd2-11b2-8080-808080808080' is automatically used if the create request is sent by user with 'SYS_ADMIN' authority." +
                    "Remove 'id', 'tenantId' rom the request body example (below) to create new Widget Type entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetTypeDetails saveWidgetType(
            @ApiParam(value = "A JSON value representing the Widget Type Details.", required = true)
            @RequestBody WidgetTypeDetails widgetTypeDetails) throws Exception {
        var currentUser = getCurrentUser();
        if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
        } else {
            widgetTypeDetails.setTenantId(currentUser.getTenantId());
        }

        checkEntity(widgetTypeDetails.getId(), widgetTypeDetails, Resource.WIDGET_TYPE);
        WidgetTypeDetails savedWidgetTypeDetails = widgetTypeService.saveWidgetType(widgetTypeDetails);

        if (!Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(widgetTypeDetails.getTenantId(), widgetTypeDetails.getBundleAlias());
            if (widgetsBundle != null) {
                autoCommit(currentUser, widgetsBundle.getId());
            }
        }

        sendEntityNotificationMsg(getTenantId(), savedWidgetTypeDetails.getId(),
                widgetTypeDetails.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

        return checkNotNull(savedWidgetTypeDetails);
    }

    @ApiOperation(value = "Delete widget type (deleteWidgetType)",
            notes = "Deletes the  Widget Type. Referencing non-existing Widget Type Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws Exception {
        checkParameter("widgetTypeId", strWidgetTypeId);
        var currentUser = getCurrentUser();
        WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
        WidgetTypeDetails wtd = checkWidgetTypeId(widgetTypeId, Operation.DELETE);
        widgetTypeService.deleteWidgetType(currentUser.getTenantId(), widgetTypeId);

        if (wtd != null && !Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(wtd.getTenantId(), wtd.getBundleAlias());
            if (widgetsBundle != null) {
                autoCommit(currentUser, widgetsBundle.getId());
            }
        }

        sendEntityNotificationMsg(getTenantId(), widgetTypeId, EdgeEventActionType.DELETED);
    }

    @ApiOperation(value = "Get all Widget types for specified Bundle (getBundleWidgetTypes)",
            notes = "Returns an array of Widget Type objects that belong to specified Widget Bundle." + WIDGET_TYPE_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypes", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @ApiOperation(value = "Get all Widget types details for specified Bundle (getBundleWidgetTypes)",
            notes = "Returns an array of Widget Type Details objects that belong to specified Widget Bundle." + WIDGET_TYPE_DETAILS_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypesDetails", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        return checkNotNull(widgetTypeService.findWidgetTypesDetailsByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @ApiOperation(value = "Get Widget Type Info objects (getBundleWidgetTypesInfos)",
            notes = "Get the Widget Type Info objects based on the provided parameters. " + WIDGET_TYPE_INFO_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypesInfos", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetTypeInfo> getBundleWidgetTypesInfos(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        return checkNotNull(widgetTypeService.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @ApiOperation(value = "Get Widget Type (getWidgetType)",
            notes = "Get the Widget Type based on the provided parameters. " + WIDGET_TYPE_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"isSystem", "bundleAlias", "alias"}, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias,
            @ApiParam(value = "Widget Type alias", required = true)
            @RequestParam String alias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, bundleAlias, alias);
        checkNotNull(widgetType);
        accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
        return widgetType;
    }

}
