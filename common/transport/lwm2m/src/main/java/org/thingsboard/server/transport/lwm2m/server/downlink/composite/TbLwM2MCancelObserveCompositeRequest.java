// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.Builder;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MCancelObserveCompositeRequest extends AbstractTbLwM2MTargetedDownlinkCompositeRequest {

    @Builder
    private TbLwM2MCancelObserveCompositeRequest(String [] versionedIds, long timeout) {
        super(versionedIds, timeout);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE_COMPOSITE_CANCEL;
    }
}
