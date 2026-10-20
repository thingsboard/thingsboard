// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.cf;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.entitiy.cf.CalculatedFieldReprocessingValidator.CfReprocessingValidationResult;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbCalculatedFieldService {

    CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException;

    CalculatedField findById(CalculatedFieldId calculatedFieldId, SecurityUser user);

    PageData<CalculatedField> findByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, CalculatedFieldType type, PageLink pageLink);

    void delete(CalculatedField calculatedField, User user);

    void delete(CalculatedFieldId calculatedFieldId, User user);

    CfReprocessingValidationResult validateForReprocessing(CalculatedField calculatedField);

    JsonNode executeTestScript(TenantId tenantId, JsonNode inputParams);

}
