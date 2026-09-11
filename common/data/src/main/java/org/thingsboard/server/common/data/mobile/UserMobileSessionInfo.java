// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile;

import lombok.Data;

import java.util.Map;

@Data
public class UserMobileSessionInfo {

    private Map<String, MobileSessionInfo> sessions;

}
