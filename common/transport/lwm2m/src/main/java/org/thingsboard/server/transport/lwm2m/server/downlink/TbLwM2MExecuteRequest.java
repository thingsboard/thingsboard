// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MExecuteRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ReadResponse> {

    @Getter
    private final Object params;

    @Builder
    private TbLwM2MExecuteRequest(String versionedId, long timeout, Object params) {
        super(versionedId, timeout);
        this.params = params;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.EXECUTE;
    }



}
