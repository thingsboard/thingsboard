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
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;

import java.util.List;

@Data
@Schema
@EqualsAndHashCode(callSuper = true)
public class OAuth2RegistrationInfo extends BaseData<OAuth2RegistrationId> {
    @Schema(description = "Oauth2 client registration title (e.g. Google)")
    private String title;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;

    public OAuth2RegistrationInfo() {
        super();
    }

    public OAuth2RegistrationInfo(OAuth2RegistrationId id) {
        super(id);
    }

    public OAuth2RegistrationInfo(OAuth2RegistrationId id, String title, List<PlatformType> platforms) {
        super(id);
        this.title = title;
        this.platforms = platforms;
    }

}
