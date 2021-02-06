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
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_INFO_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationInfoEntity extends AbstractOAuth2ClientRegistrationInfoEntity<OAuth2ClientRegistrationInfo> {

    public OAuth2ClientRegistrationInfoEntity() {
        super();
    }

    public OAuth2ClientRegistrationInfoEntity(OAuth2ClientRegistrationInfo clientRegistration) {
        super(clientRegistration);
    }

    public OAuth2ClientRegistrationInfoEntity(OAuth2ClientRegistrationInfoEntity oAuth2ClientRegistrationInfoEntity) {
        super(oAuth2ClientRegistrationInfoEntity);
    }

    @Override
    public OAuth2ClientRegistrationInfo toData() {
        return super.toOAuth2ClientRegistrationInfo();
    }
}
