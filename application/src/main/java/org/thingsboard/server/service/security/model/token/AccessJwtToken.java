// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.model.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import org.thingsboard.server.common.data.security.model.JwtToken;

public final class AccessJwtToken implements JwtToken {

    @Getter
    private final String token;
    @JsonIgnore
    @Getter
    private final transient Claims claims;

    AccessJwtToken(final String token, Claims claims) {
        this.token = token;
        this.claims = claims;
    }

    @Override
    public String token() {
        return this.token;
    }

}
