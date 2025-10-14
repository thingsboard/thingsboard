/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.pat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKey extends ApiKeyInfo {

    @Serial
    private static final long serialVersionUID = -2313196723950490263L;

    @NoXss
    @Schema(description = "Api key value", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    public ApiKey() {
        super();
    }

    public ApiKey(ApiKeyId id) {
        super(id);
    }

    public ApiKey(ApiKey apiKey) {
        super(apiKey);
        this.value = apiKey.getValue();
    }

    public ApiKey(ApiKeyInfo apiKeyInfo) {
        super(apiKeyInfo);
        this.value = null;
    }

    public ApiKey(ApiKeyInfo apiKeyInfo, String value) {
        super(apiKeyInfo);
        this.value = value;
    }

}
