package org.thingsboard.server.transport.lwm2m.secure.credentials;

import org.eclipse.leshan.core.SecurityMode;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;

public class NoSecClientCredentialsConfig implements LwM2MClientCredentialsConfig {

    @Override
    public SecurityMode getSecurityConfigClientMode() {
        return NO_SEC;
    }
}
