// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MDiscoverRequest extends AbstractTbLwM2MTargetedDownlinkRequest<DiscoverResponse> {

    @Builder
    private TbLwM2MDiscoverRequest(String versionedId, long timeout) {
        super(versionedId, timeout);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.DISCOVER;
    }



}
