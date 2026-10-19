// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
