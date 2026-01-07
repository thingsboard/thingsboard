/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DOMAIN_OAUTH2_CLIENT_DOMAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DOMAIN_OAUTH2_CLIENT_TABLE_NAME;

@Data
@Entity
@Table(name = DOMAIN_OAUTH2_CLIENT_TABLE_NAME)
@IdClass(DomainOauth2ClientCompositeKey.class)
public final class DomainOauth2ClientEntity implements ToData<DomainOauth2Client> {

    @Id
    @Column(name = DOMAIN_OAUTH2_CLIENT_DOMAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID domainId;

    @Id
    @Column(name = ModelConstants.DOMAIN_OAUTH2_CLIENT_CLIENT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID oauth2ClientId;


    public DomainOauth2ClientEntity() {
        super();
    }

    public DomainOauth2ClientEntity(DomainOauth2Client domainOauth2Client) {
        domainId = domainOauth2Client.getDomainId().getId();
        oauth2ClientId = domainOauth2Client.getOAuth2ClientId().getId();
    }

    @Override
    public DomainOauth2Client toData() {
        DomainOauth2Client result = new DomainOauth2Client();
        result.setDomainId(new DomainId(domainId));
        result.setOAuth2ClientId(new OAuth2ClientId(oauth2ClientId));
        return result;
    }
}
