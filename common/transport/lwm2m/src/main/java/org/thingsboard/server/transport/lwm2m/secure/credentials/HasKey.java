package org.thingsboard.server.transport.lwm2m.secure.credentials;

import org.eclipse.leshan.core.util.Hex;

public class HasKey {
    private byte[] key;

    public void setKey(String key) {
        if (key != null) {
            this.key = Hex.decodeHex(key.toLowerCase().toCharArray());
        }
    }

    public byte[] getKey() {
        return key;
    }
}
