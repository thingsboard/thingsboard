package org.thingsboard.server.transport.lwm2m.secure.credentials;

import org.eclipse.leshan.core.SecurityMode;

import static org.eclipse.leshan.core.SecurityMode.RPK;

public class RPKClientCredentialsConfig extends HasKey implements LwM2MClientCredentialsConfig {

    @Override
    public SecurityMode getSecurityConfigClientMode() {
        return RPK;
    }
}
