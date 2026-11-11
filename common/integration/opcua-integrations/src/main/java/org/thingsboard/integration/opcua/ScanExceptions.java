// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import lombok.Data;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.thingsboard.integration.api.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

@Data
class ScanExceptions {

    private OpcUaIntegrationException critical;
    private final List<OpcUaIntegrationException> other;

    public ScanExceptions() {
        this.other = new ArrayList<>();
    }

    public void add(OpcUaIntegrationException e) {
        UaException uaException = ExceptionUtil.lookupException(e.getCause(), UaException.class);
        if (uaException != null) {
            critical = e;
        } else {
            other.add(e);
        }
    }

}
