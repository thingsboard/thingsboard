// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.pat.ApiKey;

import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_VALUE_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = API_KEY_TABLE_NAME)
public class ApiKeyEntity extends AbstractApiKeyInfoEntity<ApiKey> {

    @Column(name = API_KEY_VALUE_COLUMN_NAME)
    private String value;

    public ApiKeyEntity() {
        super();
    }

    public ApiKeyEntity(ApiKey apiKey) {
        super(apiKey);
        this.value = apiKey.getValue();
    }

    @Override
    public ApiKey toData() {
        return new ApiKey(super.toApiKeyInfo(), value);
    }

}
