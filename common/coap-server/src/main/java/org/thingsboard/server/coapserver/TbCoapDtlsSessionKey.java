// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.coapserver;

import java.net.InetSocketAddress;
import java.util.Objects;

public record TbCoapDtlsSessionKey(InetSocketAddress peerAddress, String credentials) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbCoapDtlsSessionKey that = (TbCoapDtlsSessionKey) o;
        return Objects.equals(peerAddress, that.peerAddress) &&
                Objects.equals(credentials, that.credentials);
    }
}

