// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MCancelAllRequest implements TbLwM2MDownlinkRequest<Integer> {

    @Getter
    private final long timeout;

    @Builder
    private TbLwM2MCancelAllRequest(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE_CANCEL_ALL;
    }

}
