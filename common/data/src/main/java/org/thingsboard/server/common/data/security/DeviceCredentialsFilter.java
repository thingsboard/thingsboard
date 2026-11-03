// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security;

/**
 * TODO: This is a temporary name. DeviceCredentialsId is resereved in dao layer
 */
public interface DeviceCredentialsFilter {

    String getCredentialsId();

    DeviceCredentialsType getCredentialsType();

}
