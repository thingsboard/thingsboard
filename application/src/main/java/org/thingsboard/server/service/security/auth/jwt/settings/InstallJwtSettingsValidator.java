// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;

/**
 * During Install or upgrade the validation is suppressed to keep existing data
 * */

@Primary
@Profile("install")
@Component
@RequiredArgsConstructor
public class InstallJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    public void validate(JwtSettings jwtSettings) {

    }

}
