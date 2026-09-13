// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MDiscoverAllRequest implements TbLwM2MDownlinkRequest<String> {

    @Getter
    private final long timeout;

    @Builder
    private TbLwM2MDiscoverAllRequest(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.DISCOVER_ALL;
    }



}
