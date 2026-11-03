// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;
import org.thingsboard.server.transport.lwm2m.server.downlink.AbstractTbLwM2MTargetedDownlinkRequest;

public class TbLwM2MWriteCompositeRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteCompositeResponse> {

    @Getter
    private final ContentFormat contentFormat;
    @Getter
    private final Object value;

    @Builder
    private TbLwM2MWriteCompositeRequest(String versionedId, long timeout, ContentFormat contentFormat, Object value) {
        super(versionedId, timeout);
        this.contentFormat = contentFormat;
        this.value = value;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.WRITE_REPLACE;
    }



}
