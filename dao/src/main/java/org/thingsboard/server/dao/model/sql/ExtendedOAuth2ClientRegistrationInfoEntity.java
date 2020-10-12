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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.oauth2.ExtendedOAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.SchemeType;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExtendedOAuth2ClientRegistrationInfoEntity extends AbstractOAuth2ClientRegistrationInfoEntity<ExtendedOAuth2ClientRegistrationInfo> {

    private String domainName;
    private SchemeType domainScheme;

    public ExtendedOAuth2ClientRegistrationInfoEntity() {
        super();
    }

    public ExtendedOAuth2ClientRegistrationInfoEntity(OAuth2ClientRegistrationInfoEntity oAuth2ClientRegistrationInfoEntity,
                                                      String domainName,
                                                      SchemeType domainScheme) {
        super(oAuth2ClientRegistrationInfoEntity);
        this.domainName = domainName;
        this.domainScheme = domainScheme;
    }

    @Override
    public ExtendedOAuth2ClientRegistrationInfo toData() {
        return new ExtendedOAuth2ClientRegistrationInfo(super.toOAuth2ClientRegistrationInfo(),
                domainScheme,
                domainName);
    }
}
