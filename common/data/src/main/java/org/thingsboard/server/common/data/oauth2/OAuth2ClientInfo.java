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
package org.thingsboard.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2IntegrationId;

@EqualsAndHashCode(callSuper = true)
@Data
public class OAuth2ClientInfo extends BaseData<OAuth2IntegrationId> {

    private String name;
    private String icon;
    private String url;

    public OAuth2ClientInfo() {
        super();
    }

    public OAuth2ClientInfo(OAuth2IntegrationId id) {
        super(id);
    }

    public OAuth2ClientInfo(OAuth2ClientInfo oauth2ClientInfo) {
        super(oauth2ClientInfo);
        this.name = oauth2ClientInfo.getName();
        this.icon = oauth2ClientInfo.getIcon();
        this.url = oauth2ClientInfo.getUrl();
    }
}
