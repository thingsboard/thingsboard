// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnonymousCredentials implements ClientCredentials {
    @Override
    public CredentialsType getType() {
        return CredentialsType.ANONYMOUS;
    }
}
