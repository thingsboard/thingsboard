/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

@Getter
@Setter
public class X509ClientCredentials extends AbstractLwM2MClientCredentials {
    private String cert;

    private byte[] certInBytes;

    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.X509;
    }

    @SneakyThrows
    @JsonIgnore
    public byte[] getDecodedCert() throws IllegalArgumentException {
        if (certInBytes == null) {
            certInBytes = Base64.decodeBase64(cert.getBytes());
        }
        return certInBytes;
    }
}
