/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
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
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.CF_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CalculatedFieldController extends BaseController {

    private final TbCalculatedFieldService tbCalculatedFieldService;

    public static final String CALCULATED_FIELD_ID = "calculatedFieldId";

    @ApiOperation(value = "Create Or Update Calculated Field (saveCalculatedField)",
            notes = "Creates or Updates the Calculated Field. When creating calculated field, platform generates Calculated Field Id as " + UUID_WIKI_LINK +
                    "The newly created Calculated Field Id will be present in the response. " +
                    "Specify existing Calculated Field Id to update the calculated field. " +
                    "Referencing non-existing Calculated Field Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Calculated Field entity. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/calculatedField", method = RequestMethod.POST)
    @ResponseBody
    public CalculatedField saveCalculatedField(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the calculated field.")
                                               @RequestBody CalculatedField calculatedField) throws Exception {
        calculatedField.setTenantId(getTenantId());
        checkEntityId(calculatedField.getEntityId(), Operation.WRITE_CALCULATED_FIELD);
        return tbCalculatedFieldService.save(calculatedField, getCurrentUser());
    }

    @ApiOperation(value = "Get Calculated Field (getCalculatedFieldById)",
            notes = "Fetch the Calculated Field object based on the provided Calculated Field Id."
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/calculatedField/{calculatedFieldId}", method = RequestMethod.GET)
    @ResponseBody
    public CalculatedField getCalculatedFieldById(@Parameter @PathVariable(CALCULATED_FIELD_ID) String strCalculatedFieldId) throws ThingsboardException {
        checkParameter(CALCULATED_FIELD_ID, strCalculatedFieldId);
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(toUUID(strCalculatedFieldId));
        CalculatedField calculatedField = tbCalculatedFieldService.findById(calculatedFieldId, getCurrentUser());
        checkNotNull(calculatedField);
        checkEntityId(calculatedField.getEntityId(), Operation.READ_CALCULATED_FIELD);
        return calculatedField;
    }

    @ApiOperation(value = "Get Calculated Fields by Entity Id (getCalculatedFieldsByEntityId)",
            notes = "Fetch the Calculated Fields based on the provided Entity Id."
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{entityType}/{entityId}/calculatedFields", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<CalculatedField> getCalculatedFieldsByEntityId(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = CF_TEXT_SEARCH_DESCRIPTION) @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        checkParameter("entityId", entityIdStr);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityIdStr);
        checkEntityId(entityId, Operation.READ_CALCULATED_FIELD);
        return checkNotNull(tbCalculatedFieldService.findAllByTenantIdAndEntityId(entityId, getCurrentUser(), pageLink));
    }

    @ApiOperation(value = "Delete Calculated Field (deleteCalculatedField)",
            notes = "Deletes the calculated field. Referencing non-existing Calculated Field Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/calculatedField/{calculatedFieldId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCalculatedField(@PathVariable(CALCULATED_FIELD_ID) String strCalculatedField) throws Exception {
        checkParameter(CALCULATED_FIELD_ID, strCalculatedField);
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(toUUID(strCalculatedField));
        CalculatedField calculatedField = tbCalculatedFieldService.findById(calculatedFieldId, getCurrentUser());
        checkEntityId(calculatedField.getEntityId(), Operation.WRITE_CALCULATED_FIELD);
        tbCalculatedFieldService.delete(calculatedField, getCurrentUser());
    }

}
