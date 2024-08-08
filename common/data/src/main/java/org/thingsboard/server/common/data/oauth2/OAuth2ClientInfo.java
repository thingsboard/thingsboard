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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2ClientId;

import java.util.List;

@Data
@Schema
@EqualsAndHashCode(callSuper = true)
public class OAuth2ClientInfo extends BaseData<OAuth2ClientId> {

    @Schema(description = "Oauth2 client registration title (e.g. Google)")
    private String title;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;

    public OAuth2ClientInfo() {
        super();
    }

    public OAuth2ClientInfo(OAuth2ClientId id) {
        super(id);
    }

    public OAuth2ClientInfo(OAuth2Client oAuth2Client) {
        super(oAuth2Client);
        this.title = oAuth2Client.getTitle();
        this.platforms = oAuth2Client.getPlatforms();
    }

}
