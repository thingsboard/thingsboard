// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model;

import java.io.Serializable;

public interface JwtToken extends Serializable {
    String getToken();
}
