// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class OAuth2ClientMapperProvider {

    @Autowired
    @Qualifier("basicOAuth2ClientMapper")
    private OAuth2ClientMapper basicOAuth2ClientMapper;

    @Autowired
    @Qualifier("customOAuth2ClientMapper")
    private OAuth2ClientMapper customOAuth2ClientMapper;

    @Autowired
    @Qualifier("githubOAuth2ClientMapper")
    private OAuth2ClientMapper githubOAuth2ClientMapper;

    @Autowired
    @Qualifier("appleOAuth2ClientMapper")
    private OAuth2ClientMapper appleOAuth2ClientMapper;

    public OAuth2ClientMapper getOAuth2ClientMapperByType(MapperType oauth2MapperType) {
        switch (oauth2MapperType) {
            case CUSTOM:
                return customOAuth2ClientMapper;
            case BASIC:
                return basicOAuth2ClientMapper;
            case GITHUB:
                return githubOAuth2ClientMapper;
            case APPLE:
                return appleOAuth2ClientMapper;
            default:
                throw new RuntimeException("OAuth2ClientRegistrationMapper with type " + oauth2MapperType + " is not supported!");
        }
    }
}
