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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Slf4j
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaOAuth2ClientRegistrationDao implements OAuth2ClientRegistrationDao {
    private final OAuth2ClientRegistrationRepository repository;

    @Override
    @Transactional
    public OAuth2ClientRegistration save(OAuth2ClientRegistration clientRegistration) {
        OAuth2ClientRegistrationEntity entity;
        try {
            entity = new OAuth2ClientRegistrationEntity(clientRegistration);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", clientRegistration, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + clientRegistration + "}", e);
        }
        log.debug("Saving entity {}", entity);
        entity = repository.save(entity);
        return DaoUtil.getData(entity);
    }

    @Override
    public List<OAuth2ClientRegistration> find() {
        List<OAuth2ClientRegistrationEntity> entities = Lists.newArrayList(repository.findAll());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public OAuth2ClientRegistration findById(String registrationId) {
        log.debug("Get entity by registration id {}", registrationId);
        Optional<OAuth2ClientRegistrationEntity> entity = repository.findByRegistrationId(registrationId);
        return DaoUtil.getData(entity);
    }

    @Override
    public boolean removeById(String registrationId) {
        repository.deleteByRegistrationId(registrationId);
        log.debug("Remove request: {}", registrationId);
        return !repository.existsByRegistrationId(registrationId);
    }
}
