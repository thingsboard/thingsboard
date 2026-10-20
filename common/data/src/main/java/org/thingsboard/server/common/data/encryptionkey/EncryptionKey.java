// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.encryptionkey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class EncryptionKey extends BaseData<EncryptionKeyId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the secret cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @NotBlank
    @Length(fieldName = "password")
    @Schema(description = "Encryption key password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Password")
    private String password;

    @NoXss
    @NotBlank
    @Length(fieldName = "salt")
    @Schema(description = "Encryption key salt", requiredMode = Schema.RequiredMode.REQUIRED, example = "Salt")
    private String salt;

    public EncryptionKey() {
        super();
    }

    public EncryptionKey(EncryptionKeyId id) {
        super(id);
    }

    public EncryptionKey(EncryptionKey encryptionKey) {
        super(encryptionKey);
        this.tenantId = encryptionKey.getTenantId();
        this.password = encryptionKey.getPassword();
        this.salt = encryptionKey.getSalt();
    }

}
