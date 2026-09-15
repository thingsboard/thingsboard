// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

import java.util.Optional;

public class TbLwM2MReadRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ReadResponse> implements HasContentFormat {

    private final Optional<ContentFormat> requestContentFormat;

    @Builder
    private TbLwM2MReadRequest(String versionedId, long timeout, ContentFormat requestContentFormat) {
        super(versionedId, timeout);
        this.requestContentFormat = Optional.ofNullable(requestContentFormat);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.READ;
    }


    @Override
    public Optional<ContentFormat> getRequestContentFormat() {
        return this.requestContentFormat;
    }
}
