// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public class TbLwM2MWriteAttributesRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteAttributesResponse> {

    @Getter
    private final ObjectAttributes attributes;

    @Builder
    private TbLwM2MWriteAttributesRequest(String versionedId, long timeout, ObjectAttributes attributes) {
        super(versionedId, timeout);
        this.attributes = attributes;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.WRITE_ATTRIBUTES;
    }



}
