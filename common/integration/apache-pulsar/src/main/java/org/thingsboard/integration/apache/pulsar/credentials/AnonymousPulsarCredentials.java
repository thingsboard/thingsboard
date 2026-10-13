// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar.credentials;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.impl.auth.AuthenticationDisabled;

public class AnonymousPulsarCredentials implements PulsarCredentials {
    @Override
    public Authentication getAuthentication() {
        return new AuthenticationDisabled();
    }
}
