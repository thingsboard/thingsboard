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
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.mobile.MobileAppOauth2Client;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_OAUTH2_CLIENT_MOBILE_APP_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_OAUTH2_CLIENT_CLIENT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_OAUTH2_CLIENT_TABLE_NAME;

@Data
@Entity
@Table(name = MOBILE_APP_OAUTH2_CLIENT_TABLE_NAME)
@IdClass(MobileAppOauth2ClientCompositeKey.class)
public final class MobileAppOauth2ClientEntity implements ToData<MobileAppOauth2Client> {

    @Id
    @Column(name = MOBILE_APP_OAUTH2_CLIENT_MOBILE_APP_ID_PROPERTY, columnDefinition = "uuid")
    private UUID mobileAppId;

    @Id
    @Column(name = MOBILE_APP_OAUTH2_CLIENT_CLIENT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID oauth2ClientId;

    public MobileAppOauth2ClientEntity() {
        super();
    }

    public MobileAppOauth2ClientEntity(MobileAppOauth2Client domainOauth2Provider) {
        mobileAppId = domainOauth2Provider.getMobileAppId().getId();
        oauth2ClientId = domainOauth2Provider.getOAuth2ClientId().getId();
    }

    @Override
    public MobileAppOauth2Client toData() {
        MobileAppOauth2Client result = new MobileAppOauth2Client();
        result.setMobileAppId(new MobileAppId(mobileAppId));
        result.setOAuth2ClientId(new OAuth2ClientId(oauth2ClientId));
        return result;
    }
}
