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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class CalculatedFieldLinkDataValidator extends DataValidator<CalculatedFieldLink> {

    @Autowired
    private CalculatedFieldLinkDao calculatedFieldLinkDao;

    @Override
    protected CalculatedFieldLink validateUpdate(TenantId tenantId, CalculatedFieldLink calculatedFieldLink) {
        CalculatedFieldLink old = calculatedFieldLinkDao.findById(calculatedFieldLink.getTenantId(), calculatedFieldLink.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing calculated field link!");
        }
        return old;
    }

}
