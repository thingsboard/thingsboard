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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleFilter;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.widgets.bundle.TbWidgetsBundleService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_BUNDLE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_BUNDLE_TEXT_SEARCH_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class WidgetsBundleController extends BaseController {

    private final TbWidgetsBundleService tbWidgetsBundleService;
    private final ImageService imageService;

    private static final String WIDGET_BUNDLE_DESCRIPTION = "Widget Bundle represents a group(bundle) of widgets. Widgets are grouped into bundle by type or use case. ";
    private static final String FULL_SEARCH_PARAM_DESCRIPTION = "Optional boolean parameter indicating extended search of widget bundles by description and by name / description of related widget types";
    private static final String SCADA_FIRST_PARAM_DESCRIPTION = "Optional boolean parameter indicating whether to fetch widgets bundles with SCADA symbols first. Works only when fullSearch parameter is enabled";
    private static final String TENANT_BUNDLES_ONLY_DESCRIPTION = "Optional boolean parameter to include only tenant-level bundles without system";

    @ApiOperation(value = "Get Widget Bundle (getWidgetsBundleById)",
            notes = "Get the Widget Bundle based on the provided Widget Bundle Id. " + WIDGET_BUNDLE_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/widgetsBundle/{widgetsBundleId}")
    public WidgetsBundle getWidgetsBundleById(
            @Parameter(description = WIDGET_BUNDLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetsBundleId") String strWidgetsBundleId,
            @Parameter(description = INLINE_IMAGES_DESCRIPTION)
            @RequestParam(value = INLINE_IMAGES, required = false) boolean inlineImages) throws ThingsboardException {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        var result = checkWidgetsBundleId(widgetsBundleId, Operation.READ);
        if (inlineImages) {
            result = imageService.inlineImage(result);
        }
        return result;
    }

    @ApiOperation(value = "Create Or Update Widget Bundle (saveWidgetsBundle)",
            notes = "Create or update the Widget Bundle. " + WIDGET_BUNDLE_DESCRIPTION + " " +
                    "When creating the bundle, platform generates Widget Bundle Id as " + UUID_WIKI_LINK +
                    "The newly created Widget Bundle Id will be present in the response. " +
                    "Specify existing Widget Bundle id to update the Widget Bundle. " +
                    "Referencing non-existing Widget Bundle Id will cause 'Not Found' error." +
                    "\n\nWidget Bundle alias is unique in the scope of tenant. " +
                    "Special Tenant Id '13814000-1dd2-11b2-8080-808080808080' is automatically used if the create bundle request is sent by user with 'SYS_ADMIN' authority." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Widgets Bundle entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/widgetsBundle")
    public WidgetsBundle saveWidgetsBundle(
            @Parameter(description = "A JSON value representing the Widget Bundle.", required = true)
            @RequestBody WidgetsBundle widgetsBundle) throws Exception {
        var currentUser = getCurrentUser();
        if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
        } else {
            widgetsBundle.setTenantId(currentUser.getTenantId());
        }

        checkEntity(widgetsBundle.getId(), widgetsBundle, Resource.WIDGETS_BUNDLE);

        return tbWidgetsBundleService.save(widgetsBundle, currentUser);
    }

    @ApiOperation(value = "Update widgets bundle widgets types list (updateWidgetsBundleWidgetTypes)",
            notes = "Updates widgets bundle widgets list." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/widgetsBundle/{widgetsBundleId}/widgetTypes")
    @ResponseStatus(value = HttpStatus.OK)
    public void updateWidgetsBundleWidgetTypes(
            @Parameter(description = WIDGET_BUNDLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetsBundleId") String strWidgetsBundleId,
            @Parameter(description = "Ordered list of widget type Ids to be included by widgets bundle")
            @RequestBody List<String> strWidgetTypeIds) throws Exception {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        checkNotNull(strWidgetTypeIds);
        Set<WidgetTypeId> widgetTypeIds = new LinkedHashSet<>();
        var currentUser = getCurrentUser();
        TenantId tenantId = currentUser.getTenantId();
        for (String strWidgetTypeId : strWidgetTypeIds) {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            if (!widgetTypeIds.contains(widgetTypeId) &&
                    widgetTypeService.widgetTypeExistsByTenantIdAndWidgetTypeId(tenantId, widgetTypeId)) {
                widgetTypeIds.add(widgetTypeId);
            }
        }
        tbWidgetsBundleService.updateWidgetsBundleWidgetTypes(widgetsBundleId, new ArrayList<>(widgetTypeIds), currentUser);
    }

    @ApiOperation(value = "Update widgets bundle widgets list from widget type FQNs list (updateWidgetsBundleWidgetFqns)",
            notes = "Updates widgets bundle widgets list from widget type FQNs list." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/widgetsBundle/{widgetsBundleId}/widgetTypeFqns")
    @ResponseStatus(value = HttpStatus.OK)
    public void updateWidgetsBundleWidgetFqns(
            @Parameter(description = WIDGET_BUNDLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetsBundleId") String strWidgetsBundleId,
            @Parameter(description = "Ordered list of widget type FQNs to be included by widgets bundle")
            @RequestBody List<String> widgetTypeFqns) throws Exception {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        checkNotNull(widgetTypeFqns);
        var currentUser = getCurrentUser();
        tbWidgetsBundleService.updateWidgetsBundleWidgetFqns(widgetsBundleId, widgetTypeFqns, currentUser);
    }

    @ApiOperation(value = "Delete widgets bundle (deleteWidgetsBundle)",
            notes = "Deletes the widget bundle. Referencing non-existing Widget Bundle Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/widgetsBundle/{widgetsBundleId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetsBundle(
            @Parameter(description = WIDGET_BUNDLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetsBundleId") String strWidgetsBundleId) throws ThingsboardException {
        checkParameter("widgetsBundleId", strWidgetsBundleId);
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        WidgetsBundle widgetsBundle = checkWidgetsBundleId(widgetsBundleId, Operation.DELETE);
        tbWidgetsBundleService.delete(widgetsBundle, getCurrentUser());
    }

    @ApiOperation(value = "Get Widget Bundles (getWidgetsBundles)",
            notes = "Returns a page of Widget Bundle objects available for current user. " + WIDGET_BUNDLE_DESCRIPTION + " " +
                    PAGE_DATA_PARAMETERS + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/widgetsBundles", params = {"pageSize", "page"})
    public PageData<WidgetsBundle> getWidgetsBundles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = WIDGET_BUNDLE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "tenantId"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = TENANT_BUNDLES_ONLY_DESCRIPTION)
            @RequestParam(required = false) Boolean tenantOnly,
            @Parameter(description = FULL_SEARCH_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean fullSearch,
            @Parameter(description = SCADA_FIRST_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean scadaFirst) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        WidgetsBundleFilter widgetsBundleFilter = WidgetsBundleFilter.builder()
                .tenantId(getTenantId())
                .fullSearch(fullSearch != null && fullSearch)
                .scadaFirst(scadaFirst != null && scadaFirst)
                .build();
        if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(widgetsBundleService.findSystemWidgetsBundlesByPageLink(widgetsBundleFilter, pageLink));
        } else {
            if (tenantOnly != null && tenantOnly) {
                return checkNotNull(widgetsBundleService.findTenantWidgetsBundlesByTenantIdAndPageLink(widgetsBundleFilter, pageLink));
            } else {
                return checkNotNull(widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(widgetsBundleFilter, pageLink));
            }
        }
    }

    @ApiOperation(value = "Get all Widget Bundles (getWidgetsBundles)",
            notes = "Returns an array of Widget Bundle objects that are available for current user." + WIDGET_BUNDLE_DESCRIPTION + " " + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/widgetsBundles")
    public List<WidgetsBundle> getWidgetsBundles() throws ThingsboardException {
        if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(widgetsBundleService.findSystemWidgetsBundles(getTenantId()));
        } else {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
        }
    }

    @ApiOperation(value = "Get Widgets Bundles By Ids (getWidgetsBundlesByIds)",
            notes = "Requested widgets bundles must be system level or owned by tenant of the user which is performing the request. " +
                    NEW_LINE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/widgetsBundles", params = {"widgetsBundleIds"})
    public List<WidgetsBundle> getWidgetsBundlesByIds(
            @Parameter(description = "A list of widgets bundle ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("widgetsBundleIds") Set<UUID> widgetsBundleUUIDs) throws ThingsboardException {
        List<WidgetsBundleId> widgetsBundleIds = new ArrayList<>();
        for (UUID widgetsBundleUUID : widgetsBundleUUIDs) {
            widgetsBundleIds.add(new WidgetsBundleId(widgetsBundleUUID));
        }
        return widgetsBundleService.findSystemOrTenantWidgetsBundlesByIds(getTenantId(), widgetsBundleIds);
    }

}
