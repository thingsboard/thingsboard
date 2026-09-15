// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.oauth2;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.validation.Length;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString(exclude = {"password"})
public class OAuth2CustomMapperConfig {
    @Length(fieldName = "url")
    private final String url;
    @Length(fieldName = "username")
    private final String username;
    @Length(fieldName = "password")
    private final String password;
    private final boolean sendToken;
}
