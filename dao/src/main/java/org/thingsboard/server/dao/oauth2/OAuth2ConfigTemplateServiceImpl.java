/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.oauth2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
@AllArgsConstructor
public class OAuth2ConfigTemplateServiceImpl extends AbstractEntityService implements OAuth2ConfigTemplateService {

    private static final String INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID = "Incorrect clientRegistrationTemplateId ";
    private static final String INCORRECT_CLIENT_REGISTRATION_PROVIDER_ID = "Incorrect clientRegistrationProviderId ";

    private final OAuth2ClientRegistrationTemplateDao clientRegistrationTemplateDao;
    private final DataValidator<OAuth2ClientRegistrationTemplate> clientRegistrationTemplateValidator;

    @Override
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        log.trace("Executing saveClientRegistrationTemplate [{}]", clientRegistrationTemplate);
        clientRegistrationTemplateValidator.validate(clientRegistrationTemplate, o -> TenantId.SYS_TENANT_ID);
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate;
        try {
            savedClientRegistrationTemplate = clientRegistrationTemplateDao.save(TenantId.SYS_TENANT_ID, clientRegistrationTemplate);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("oauth2_template_provider_id_unq_key")) {
                throw new DataValidationException("Client registration template with such providerId already exists!");
            } else {
                throw t;
            }
        }
        return savedClientRegistrationTemplate;
    }

    @Override
    public Optional<OAuth2ClientRegistrationTemplate> findClientRegistrationTemplateByProviderId(String providerId) {
        log.trace("Executing findClientRegistrationTemplateByProviderId [{}]", providerId);
        validateString(providerId, id -> INCORRECT_CLIENT_REGISTRATION_PROVIDER_ID + id);
        return clientRegistrationTemplateDao.findByProviderId(providerId);
    }

    @Override
    public OAuth2ClientRegistrationTemplate findClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId) {
        log.trace("Executing findClientRegistrationTemplateById [{}]", templateId);
        validateId(templateId, id -> INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID + id);
        return clientRegistrationTemplateDao.findById(TenantId.SYS_TENANT_ID, templateId.getId());
    }

    @Override
    public List<OAuth2ClientRegistrationTemplate> findAllClientRegistrationTemplates() {
        log.trace("Executing findAllClientRegistrationTemplates");
        return clientRegistrationTemplateDao.findAll();
    }

    @Override
    public void deleteClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId) {
        log.trace("Executing deleteClientRegistrationTemplateById [{}]", templateId);
        validateId(templateId, id -> INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID + id);
        clientRegistrationTemplateDao.removeById(TenantId.SYS_TENANT_ID, templateId.getId());
    }
}
