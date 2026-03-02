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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;

import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = API_KEY_TABLE_NAME)
public class ApiKeyInfoEntity extends AbstractApiKeyInfoEntity<ApiKeyInfo> {

    public ApiKeyInfoEntity() {
        super();
    }

    public ApiKeyInfoEntity(ApiKey apiKey) {
        super(apiKey);
    }

    @Override
    public ApiKeyInfo toData() {
        return super.toApiKeyInfo();
    }

}
