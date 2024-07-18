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
package org.thingsboard.server.common.data.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.oauth2.HasOauth2Registrations;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@AllArgsConstructor
@Builder
@Schema
public class DomainInfo extends Domain implements HasOauth2Registrations {

    @Schema(description = "List of available oauth2 client registration")
    private List<OAuth2RegistrationInfo> oauth2RegistrationInfos;

    public DomainInfo(Domain domain, List<OAuth2RegistrationInfo> oauth2RegistrationInfos) {
        super(domain);
        this.oauth2RegistrationInfos = oauth2RegistrationInfos;
    }
}
