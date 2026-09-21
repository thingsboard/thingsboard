// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
