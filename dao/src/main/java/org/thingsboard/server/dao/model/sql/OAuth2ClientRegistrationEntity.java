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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.Arrays;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationEntity extends BaseSqlEntity<OAuth2ClientRegistration> {

    @Column(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_INFO_ID_PROPERTY, columnDefinition = "uuid")
    private UUID clientRegistrationInfoId;

    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    public OAuth2ClientRegistrationEntity() {
        super();
    }

    public OAuth2ClientRegistrationEntity(OAuth2ClientRegistration clientRegistration) {
        if (clientRegistration.getId() != null) {
            this.setUuid(clientRegistration.getId().getId());
        }
        if (clientRegistration.getClientRegistrationId() != null){
            this.clientRegistrationInfoId = clientRegistration.getClientRegistrationId().getId();
        }
        this.createdTime = clientRegistration.getCreatedTime();
        this.domainName = clientRegistration.getDomainName();
        this.domainScheme = clientRegistration.getDomainScheme();
    }

    @Override
    public OAuth2ClientRegistration toData() {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setId(new OAuth2ClientRegistrationId(id));
        clientRegistration.setClientRegistrationId(new OAuth2ClientRegistrationInfoId(clientRegistrationInfoId));
        clientRegistration.setCreatedTime(createdTime);
        clientRegistration.setDomainName(domainName);
        clientRegistration.setDomainScheme(domainScheme);
        return clientRegistration;
    }
}
