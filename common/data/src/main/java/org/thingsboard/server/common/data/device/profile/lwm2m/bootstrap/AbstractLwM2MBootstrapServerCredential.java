// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

@Getter
@Setter
public abstract class AbstractLwM2MBootstrapServerCredential extends LwM2MServerSecurityConfig implements LwM2MBootstrapServerCredential {

    @JsonIgnore
    public byte[] getDecodedCServerPublicKey() {
        return getDecoded(serverPublicKey);
    }

    @SneakyThrows
    private static byte[] getDecoded(String key) {
        return Base64.decodeBase64(key.getBytes());
    }
}
