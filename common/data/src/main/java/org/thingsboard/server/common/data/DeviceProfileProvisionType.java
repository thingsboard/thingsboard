// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

public enum DeviceProfileProvisionType {
    DISABLED,
    ALLOW_CREATE_NEW_DEVICES,
    CHECK_PRE_PROVISIONED_DEVICES,
    X509_CERTIFICATE_CHAIN
}
