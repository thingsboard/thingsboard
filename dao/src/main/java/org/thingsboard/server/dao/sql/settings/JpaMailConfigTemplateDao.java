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
package org.thingsboard.server.dao.sql.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.mail.MailConfigTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.MailConfigTemplateEntity;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationTemplateEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationTemplateDao;
import org.thingsboard.server.dao.settings.MailConfigTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.oauth2.OAuth2ClientRegistrationTemplateRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaMailConfigTemplateDao extends JpaAbstractDao<MailConfigTemplateEntity, MailConfigTemplate> implements MailConfigTemplateDao {
    private final MailConfigTemplateRepository repository;

    @Override
    protected Class<MailConfigTemplateEntity> getEntityClass() {
        return MailConfigTemplateEntity.class;
    }

    @Override
    protected JpaRepository<MailConfigTemplateEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public Optional<MailConfigTemplate> findByProviderId(String providerId) {
        MailConfigTemplate mailConfigTemplate = DaoUtil.getData(repository.findByProviderId(providerId));
        return Optional.ofNullable(mailConfigTemplate);
    }

    @Override
    public List<MailConfigTemplate> findAll() {
        Iterable<MailConfigTemplateEntity> entities = repository.findAll();
        List<MailConfigTemplate> result = new ArrayList<>();
        entities.forEach(entity -> result.add(DaoUtil.getData(entity)));
        return result;
    }
}
