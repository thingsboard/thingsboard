// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.model.token;

import org.thingsboard.server.common.data.security.model.JwtToken;

public record AccessJwtToken(String token) implements JwtToken {

}
