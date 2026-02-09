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

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class OAuth2ClientInfoEntity extends BaseSqlEntity<OAuth2ClientInfo> {

    private String platforms;
    private String title;

    public OAuth2ClientInfoEntity() {
        super();
    }

    public OAuth2ClientInfoEntity(UUID id, long createdTime, String platforms, String title) {
        this.id = id;
        this.createdTime = createdTime;
        this.platforms = platforms;
        this.title = title;
    }

    @Override
    public OAuth2ClientInfo toData() {
        OAuth2ClientInfo oAuth2ClientInfo = new OAuth2ClientInfo();
        oAuth2ClientInfo.setId(new OAuth2ClientId(id));
        oAuth2ClientInfo.setCreatedTime(createdTime);
        oAuth2ClientInfo.setTitle(title);
        oAuth2ClientInfo.setPlatforms(StringUtils.isNotEmpty(platforms) ? Arrays.stream(platforms.split(","))
                .map(str -> PlatformType.valueOf(str)).collect(Collectors.toList()) : Collections.emptyList());
        return oAuth2ClientInfo;
    }
}
