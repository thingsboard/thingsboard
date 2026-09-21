// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class EventDataValidator extends DataValidator<Event> {

    @Override
    protected void validateDataImpl(TenantId tenantId, Event event) {
        if (event.getTenantId() == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (event.getEntityId() == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        if (StringUtils.isEmpty(event.getServiceId())) {
            throw new DataValidationException("Service id should be specified!.");
        }
    }
}
