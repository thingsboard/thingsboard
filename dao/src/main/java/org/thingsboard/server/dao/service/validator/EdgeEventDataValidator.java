// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class EdgeEventDataValidator extends DataValidator<EdgeEvent> {

    @Override
    protected void validateDataImpl(TenantId tenantId, EdgeEvent edgeEvent) {
        if (edgeEvent.getEdgeId() == null) {
            throw new DataValidationException("Edge id should be specified!");
        }
        if (edgeEvent.getAction() == null) {
            throw new DataValidationException("Edge Event action should be specified!");
        }
    }
}
