// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mail;

public enum MailOauth2Provider {
    GOOGLE("Google"), OFFICE_365("Office 365"), SENDGRID("SendGrid"), CUSTOM("Custom");

    public final String label;

    MailOauth2Provider(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}