// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth;

import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;

public interface DeviceAuthService {

    DeviceAuthResult process(DeviceCredentialsFilter credentials);

}
