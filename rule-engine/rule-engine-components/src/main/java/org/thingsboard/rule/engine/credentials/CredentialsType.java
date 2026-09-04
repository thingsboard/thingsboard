// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.credentials;

public enum CredentialsType {
    ANONYMOUS("anonymous"),
    BASIC("basic"),
    SAS("sas"),
    CERT_PEM("cert.PEM");

    private final String label;

    CredentialsType(String label) {
        this.label = label;
    }
}
