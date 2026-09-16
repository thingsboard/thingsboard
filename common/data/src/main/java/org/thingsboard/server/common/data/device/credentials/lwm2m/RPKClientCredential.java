// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

public class RPKClientCredential extends AbstractLwM2MClientSecurityCredential {

    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.RPK;
    }

    @Override
    public byte[] getDecoded() throws IllegalArgumentException, DecoderException {
        if (securityInBytes == null) {
            securityInBytes = Base64.decodeBase64(key.getBytes());
        }
        return securityInBytes;
    }
}
