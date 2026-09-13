// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    @Schema(description = "API key value", requiredMode = Schema.RequiredMode.REQUIRED)
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
