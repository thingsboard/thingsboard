// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

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
