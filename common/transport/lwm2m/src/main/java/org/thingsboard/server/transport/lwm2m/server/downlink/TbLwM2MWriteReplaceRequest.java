// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MWriteReplaceRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteResponse> {

    @Getter
    private final ContentFormat contentFormat;
    @Getter
    private final Object value;

    @Builder
    private TbLwM2MWriteReplaceRequest(String versionedId, long timeout, ContentFormat contentFormat, Object value) {
        super(versionedId, timeout);
        this.contentFormat = contentFormat;
        this.value = value;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.WRITE_REPLACE;
    }



}
