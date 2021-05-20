/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationTemplateEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaOAuth2ClientRegistrationTemplateDao extends JpaAbstractDao<OAuth2ClientRegistrationTemplateEntity, OAuth2ClientRegistrationTemplate> implements OAuth2ClientRegistrationTemplateDao {
    private final OAuth2ClientRegistrationTemplateRepository repository;

    @Override
    protected Class<OAuth2ClientRegistrationTemplateEntity> getEntityClass() {
        return OAuth2ClientRegistrationTemplateEntity.class;
    }

    @Override
    protected CrudRepository<OAuth2ClientRegistrationTemplateEntity, UUID> getCrudRepository() {
        return repository;
    }

    @Override
    public Optional<OAuth2ClientRegistrationTemplate> findByProviderId(String providerId) {
        OAuth2ClientRegistrationTemplate oAuth2ClientRegistrationTemplate = DaoUtil.getData(repository.findByProviderId(providerId));
        return Optional.ofNullable(oAuth2ClientRegistrationTemplate);
    }

    @Override
    public List<OAuth2ClientRegistrationTemplate> findAll() {
        Iterable<OAuth2ClientRegistrationTemplateEntity> entities = repository.findAll();
        List<OAuth2ClientRegistrationTemplate> result = new ArrayList<>();
        entities.forEach(entity -> result.add(DaoUtil.getData(entity)));
        return result;
    }
}
