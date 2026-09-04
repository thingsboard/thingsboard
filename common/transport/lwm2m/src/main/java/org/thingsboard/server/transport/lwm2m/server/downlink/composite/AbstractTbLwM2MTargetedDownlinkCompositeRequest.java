// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.Getter;
import org.thingsboard.server.transport.lwm2m.server.downlink.HasVersionedIds;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDownlinkRequest;

public abstract class AbstractTbLwM2MTargetedDownlinkCompositeRequest<T> implements TbLwM2MDownlinkRequest<T>, HasVersionedIds {

    @Getter
    private final String [] versionedIds;
    @Getter
    private final long timeout;

    public AbstractTbLwM2MTargetedDownlinkCompositeRequest(String [] versionedIds, long timeout) {
        this.versionedIds = versionedIds;
        this.timeout = timeout;
    }

}
