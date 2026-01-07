/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.cf;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseCalculatedFieldProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<CalculatedField> calculatedFieldValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        boolean isCreated = false;
        boolean isNameUpdated = false;
        try {
            CalculatedField calculatedField = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
            if (calculatedField == null) {
                throw new RuntimeException("[{" + tenantId + "}] calculatedFieldUpdateMsg {" + calculatedFieldUpdateMsg + " } cannot be converted to calculatedField");
            }

            CalculatedField calculatedFieldById = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
            if (calculatedFieldById == null) {
                calculatedField.setCreatedTime(Uuids.unixTimestamp(calculatedFieldId.getId()));
                isCreated = true;
                calculatedField.setId(null);
            } else {
                calculatedField.setId(calculatedFieldId);
            }

            String calculatedFieldName = calculatedField.getName();
            CalculatedField calculatedFieldByName = edgeCtx.getCalculatedFieldService().findByEntityIdAndName(calculatedField.getEntityId(), calculatedFieldName);
            if (calculatedFieldByName != null && !calculatedFieldByName.getId().equals(calculatedFieldId)) {
                calculatedFieldName = calculatedFieldName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] calculatedField with name {} already exists. Renaming calculatedField name to {}",
                        tenantId, calculatedField.getName(), calculatedFieldByName.getName());
                isNameUpdated = true;
            }
            calculatedField.setName(calculatedFieldName);

            calculatedFieldValidator.validate(calculatedField, CalculatedField::getTenantId);

            if (isCreated) {
                calculatedField.setId(calculatedFieldId);
            }

            edgeCtx.getCalculatedFieldService().save(calculatedField, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process calculatedField update msg [{}]", tenantId, calculatedFieldUpdateMsg, e);
            throw e;
        }
        return Pair.of(isCreated, isNameUpdated);
    }

}
