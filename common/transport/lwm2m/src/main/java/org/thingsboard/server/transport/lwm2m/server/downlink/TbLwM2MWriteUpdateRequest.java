// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MWriteUpdateRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteResponse> {

    @Getter
    private final Object value;
    @Getter
    private final ContentFormat objectContentFormat;

    @Builder
    private TbLwM2MWriteUpdateRequest(String versionedId, long timeout, Object value, ContentFormat objectContentFormat) {
        super(versionedId, timeout);
        this.value = value;
        this.objectContentFormat = objectContentFormat;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.WRITE_UPDATE;
    }



}
