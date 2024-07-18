/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class Oauth2RegistrationDataValidator extends DataValidator<OAuth2Registration> {

    @Override
    protected void validateDataImpl(TenantId tenantId, OAuth2Registration oAuth2Registration) {
        if (StringUtils.isEmpty(oAuth2Registration.getClientId())) {
            throw new DataValidationException("Client ID should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getClientId())) {
            throw new DataValidationException("Client ID should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getClientSecret())) {
            throw new DataValidationException("Client secret should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getAuthorizationUri())) {
            throw new DataValidationException("Authorization uri should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getAccessTokenUri())) {
            throw new DataValidationException("Token uri should be specified!");
        }
        if (CollectionUtils.isEmpty(oAuth2Registration.getScope())) {
            throw new DataValidationException("Scope should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getUserNameAttributeName())) {
            throw new DataValidationException("User name attribute name should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getClientAuthenticationMethod())) {
            throw new DataValidationException("Client authentication method should be specified!");
        }
        if (StringUtils.isEmpty(oAuth2Registration.getLoginButtonLabel())) {
            throw new DataValidationException("Login button label should be specified!");
        }
        OAuth2MapperConfig mapperConfig = oAuth2Registration.getMapperConfig();
        if (mapperConfig == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (mapperConfig.getType() == null) {
            throw new DataValidationException("Mapper config type should be specified!");
        }
        if (mapperConfig.getType() == MapperType.BASIC) {
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
            if (basicConfig == null) {
                throw new DataValidationException("Basic config should be specified!");
            }
            if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                throw new DataValidationException("Email attribute key should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == null) {
                throw new DataValidationException("Tenant name strategy should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                    && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                throw new DataValidationException("Tenant name pattern should be specified!");
            }
        }
        if (mapperConfig.getType() == MapperType.GITHUB) {
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
            if (basicConfig == null) {
                throw new DataValidationException("Basic config should be specified!");
            }
            if (!StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                throw new DataValidationException("Email attribute key cannot be configured for GITHUB mapper type!");
            }
            if (basicConfig.getTenantNameStrategy() == null) {
                throw new DataValidationException("Tenant name strategy should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                    && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                throw new DataValidationException("Tenant name pattern should be specified!");
            }
        }
        if (mapperConfig.getType() == MapperType.CUSTOM) {
            OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
            if (customConfig == null) {
                throw new DataValidationException("Custom config should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getUrl())) {
                throw new DataValidationException("Custom mapper URL should be specified!");
            }
        }
    }
}
