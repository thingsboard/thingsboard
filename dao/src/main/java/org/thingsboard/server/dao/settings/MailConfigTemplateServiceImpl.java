/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.settings;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.MailConfigTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mail.MailConfigTemplate;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
@AllArgsConstructor
public class MailConfigTemplateServiceImpl extends AbstractEntityService implements MailConfigTemplateService {

    private static final String INCORRECT_MAIL_CONFIG_TEMPLATE_ID = "Incorrect mailCofigTemplateId ";
    private static final String INCORRECT_MAIL_CONFIG_PROVIDER_ID = "Incorrect mailConfigProviderId ";

    private final MailConfigTemplateDao mailConfigTemplateDao;
    private final DataValidator<MailConfigTemplate> mailConfigTemplateDataValidator;


    @Override
    public MailConfigTemplate saveMailConfigTemplate(MailConfigTemplate mailConfigTemplate) {
        log.trace("Executing saveClientRegistrationTemplate [{}]", mailConfigTemplate);
        mailConfigTemplateDataValidator.validate(mailConfigTemplate, o -> TenantId.SYS_TENANT_ID);
        MailConfigTemplate savedMailConfigTemplate;
        try {
            savedMailConfigTemplate = mailConfigTemplateDao.save(TenantId.SYS_TENANT_ID, mailConfigTemplate);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("oauth2_template_provider_id_unq_key")) {
                throw new DataValidationException("Client registration template with such providerId already exists!");
            } else {
                throw t;
            }
        }
        return savedMailConfigTemplate;
    }

    @Override
    public Optional<MailConfigTemplate> findMailConfigTemplateByProviderId(String providerId) {
        log.trace("Executing findClientRegistrationTemplateByProviderId [{}]", providerId);
        validateString(providerId, INCORRECT_MAIL_CONFIG_PROVIDER_ID + providerId);
        return mailConfigTemplateDao.findByProviderId(providerId);
    }

    @Override
    public MailConfigTemplate findMailConfigTemplateById(MailConfigTemplateId templateId) {
        log.trace("Executing findClientRegistrationTemplateById [{}]", templateId);
        validateId(templateId, INCORRECT_MAIL_CONFIG_TEMPLATE_ID + templateId);
        return mailConfigTemplateDao.findById(TenantId.SYS_TENANT_ID, templateId.getId());
    }

    @Override
    public List<MailConfigTemplate> findAllMailConfigTemplates() {
        log.trace("Executing findAllClientRegistrationTemplates");
        return mailConfigTemplateDao.findAll();
    }

    @Override
    public void deleteMailConfigTemplateById(MailConfigTemplateId templateId) {

    }
}
