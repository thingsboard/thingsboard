package org.thingsboard.server.transport.lwm2m.secure.credentials;

import lombok.Data;
import org.eclipse.leshan.core.SecurityMode;

import static org.eclipse.leshan.core.SecurityMode.X509;

@Data
public class X509ClientCredentialsConfig implements LwM2MClientCredentialsConfig {
    private boolean allowTrustedOnly;
    private String cert;

    @Override
    public SecurityMode getSecurityConfigClientMode() {
        return X509;
    }
}
