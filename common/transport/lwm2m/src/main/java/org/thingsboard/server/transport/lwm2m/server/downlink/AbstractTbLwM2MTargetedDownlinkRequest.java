// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Getter;

public abstract class AbstractTbLwM2MTargetedDownlinkRequest<T> implements TbLwM2MDownlinkRequest<T>, HasVersionedId {

    @Getter
    private final String versionedId;
    @Getter
    private final long timeout;

    public AbstractTbLwM2MTargetedDownlinkRequest(String versionedId, long timeout) {
        this.versionedId = versionedId;
        this.timeout = timeout;
    }

}
