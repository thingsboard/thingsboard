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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.domain.DomainOauth2Registration;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DOMAIN_OAUTH2_PROVIDER_DOMAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DOMAIN_OAUTH2_REGISTRATION_TABLE_NAME;

@Data
@Entity
@Table(name = DOMAIN_OAUTH2_REGISTRATION_TABLE_NAME)
@IdClass(DomainOauth2RegistrationCompositeKey.class)
public final class DomainOauth2RegistrationEntity implements ToData<DomainOauth2Registration> {

    @Id
    @Column(name = DOMAIN_OAUTH2_PROVIDER_DOMAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID domainId;

    @Id
    @Column(name = ModelConstants.DOMAIN_OAUTH2_PROVIDER_PROVIDER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID oauth2RegistrationId;


    public DomainOauth2RegistrationEntity() {
        super();
    }

    public DomainOauth2RegistrationEntity(DomainOauth2Registration domainOauth2Registration) {
        domainId = domainOauth2Registration.getDomainId().getId();
        oauth2RegistrationId = domainOauth2Registration.getOAuth2RegistrationId().getId();
    }

    @Override
    public DomainOauth2Registration toData() {
        DomainOauth2Registration result = new DomainOauth2Registration();
        result.setDomainId(new DomainId(domainId));
        result.setOAuth2RegistrationId(new OAuth2RegistrationId(oauth2RegistrationId));
        return result;
    }
}
