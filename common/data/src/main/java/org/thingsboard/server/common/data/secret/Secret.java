// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.secret;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.SecretId;

import java.io.Serial;
import java.util.Base64;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Secret extends SecretInfo {

    @Serial
    private static final long serialVersionUID = 3671364019778017637L;

    @EqualsAndHashCode.Exclude
    private String value;

    @JsonIgnore
    private byte[] encryptedValue;

    @JsonGetter("encryptedValue")
    public String getEncryptedValueBase64() {
        return encryptedValue != null ? Base64.getEncoder().encodeToString(encryptedValue) : null;
    }

    @JsonSetter("encryptedValue")
    public void setEncryptedValueBase64(String value) {
        this.encryptedValue = value != null ? Base64.getDecoder().decode(value) : null;
    }


    public Secret() {
        super();
    }

    public Secret(SecretId id) {
        super(id);
    }

    public Secret(Secret secret) {
        super(secret);
        this.value = secret.getValue();
        this.encryptedValue = secret.getEncryptedValue();
    }

    public Secret(SecretInfo secretInfo) {
        super(secretInfo);
        this.value = null;
        this.encryptedValue = null;
    }

    public Secret(SecretInfo secretInfo, byte[] encryptedValue) {
        super(secretInfo);
        this.encryptedValue = encryptedValue;
    }

}
