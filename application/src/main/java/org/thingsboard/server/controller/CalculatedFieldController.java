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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldConfig;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CalculatedFieldController extends BaseController {

    private static final Set<EntityType> supportedEntityTypesForReferencedEntities = EnumSet.of(
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE);

    private final CalculatedFieldService calculatedFieldService;

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
        checkReferencedEntities(calculatedField.getConfiguration());
        return calculatedFieldService.save(calculatedField);
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
        CalculatedField calculatedField = calculatedFieldService.findById(getTenantId(), calculatedFieldId);
        checkNotNull(calculatedField);
        checkEntityId(calculatedField.getEntityId(), Operation.READ_CALCULATED_FIELD);
        return calculatedField;
    }


    @ApiOperation(value = "Delete Calculated Field (deleteCalculatedField)",
            notes = "Deletes the calculated field. Referencing non-existing Calculated Field Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/calculatedField/{calculatedFieldId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCalculatedField(@PathVariable(CALCULATED_FIELD_ID) String strCalculatedField) throws Exception {
        checkParameter(CALCULATED_FIELD_ID, strCalculatedField);
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(toUUID(strCalculatedField));
        TenantId tenantId = getTenantId();
        CalculatedField calculatedField = calculatedFieldService.findById(tenantId, calculatedFieldId);
        checkEntityId(calculatedField.getEntityId(), Operation.WRITE_CALCULATED_FIELD);
        calculatedFieldService.deleteCalculatedField(getTenantId(), calculatedFieldId);
    }

    private void checkReferencedEntities(CalculatedFieldConfig calculatedFieldConfig) throws ThingsboardException {
        List<EntityId> referencedEntityIds = calculatedFieldConfig.getArguments().values().stream()
                .map(CalculatedFieldConfig.Argument::getEntityId)
                .filter(Objects::nonNull)
                .toList();
        for (EntityId referencedEntityId : referencedEntityIds) {
            if (!supportedEntityTypesForReferencedEntities.contains(referencedEntityId.getEntityType())) {
                throw new IllegalArgumentException("Calculated fields do not support entity type '" + referencedEntityId.getEntityType() + "' for referenced entities.");
            }
            checkEntityId(referencedEntityId, Operation.READ);
        }
    }

}
