package org.thingsboard.server.transport.lwm2m.secure.credentials;

import lombok.Data;
import org.eclipse.leshan.core.SecurityMode;

import static org.eclipse.leshan.core.SecurityMode.PSK;

@Data
public class PSKClientCredentialsConfig extends HasKey implements LwM2MClientCredentialsConfig {
    private String identity;
    private String endpoint;

    @Override
    public SecurityMode getSecurityConfigClientMode() {
        return PSK;
    }
}
