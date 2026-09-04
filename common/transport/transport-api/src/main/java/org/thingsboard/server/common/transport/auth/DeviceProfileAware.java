// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth;

import org.thingsboard.server.common.data.DeviceProfile;

public interface DeviceProfileAware {

    DeviceProfile getDeviceProfile();

}
