/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaOAuth2ClientRegistrationDao extends JpaAbstractDao<OAuth2ClientRegistrationEntity, OAuth2ClientRegistration> implements OAuth2ClientRegistrationDao {
    private final OAuth2ClientRegistrationRepository repository;

    @Override
    protected Class<OAuth2ClientRegistrationEntity> getEntityClass() {
        return OAuth2ClientRegistrationEntity.class;
    }

    @Override
    protected CrudRepository<OAuth2ClientRegistrationEntity, UUID> getCrudRepository() {
        return repository;
    }

    @Override
    public OAuth2ClientRegistration findByRegistrationId(String registrationId) {
        Optional<OAuth2ClientRegistrationEntity> entity = repository.findByRegistrationId(registrationId);
        return DaoUtil.getData(entity);
    }

    @Override
    public List<OAuth2ClientRegistration> findAll() {
        Iterable<OAuth2ClientRegistrationEntity> entities = repository.findAll();
        List<OAuth2ClientRegistration> result = new ArrayList<>();
        entities.forEach(entity -> {
            result.add(DaoUtil.getData(entity));
        });
        return result;
    }

    @Override
    public List<OAuth2ClientRegistration> findByTenantId(UUID tenantId) {
        List<OAuth2ClientRegistrationEntity> entities = repository.findAllByTenantId(tenantId);
        return entities.stream().map(DaoUtil::getData).collect(Collectors.toList());
    }

    @Override
    public List<OAuth2ClientRegistration> findByDomainName(String domainName) {
        List<OAuth2ClientRegistrationEntity> entities = repository.findAllByDomainName(domainName);
        return entities.stream().map(DaoUtil::getData).collect(Collectors.toList());
    }

    @Override
    public boolean removeByRegistrationId(String registrationId) {
        repository.deleteByRegistrationId(registrationId);
        return !repository.existsByRegistrationId(registrationId);
    }

    @Override
    public int removeByTenantId(UUID tenantId) {
        return repository.deleteByTenantId(tenantId);
    }
}
