// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

@Component
public class DomainDataValidator extends AbstractHasOtaPackageValidator<Domain> {

    @Override
    protected void validateDataImpl(TenantId tenantId, Domain domain) {
        if (!isValidDomain(domain.getName())) {
            throw new IncorrectParameterException("Domain name " + domain.getName() + " is invalid");
        }
    }
}
