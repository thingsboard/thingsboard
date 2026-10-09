// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar.credentials;

import lombok.Setter;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.impl.auth.AuthenticationToken;

public class TokenPulsarCredentials implements PulsarCredentials {

    @Setter
    private String token;

    @Override
    public Authentication getAuthentication() {
        return new AuthenticationToken(token);
    }
}
