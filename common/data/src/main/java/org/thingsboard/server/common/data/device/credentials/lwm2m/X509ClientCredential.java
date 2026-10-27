// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

@Getter
@Setter
public class X509ClientCredential extends AbstractLwM2MClientSecurityCredential {

    private String cert;

    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.X509;
    }

    @Override
    public byte[] getDecoded() throws IllegalArgumentException, DecoderException {
        if (securityInBytes == null && cert != null) {
            securityInBytes = Base64.decodeBase64(cert.getBytes());
        }
        return securityInBytes;
    }
}
