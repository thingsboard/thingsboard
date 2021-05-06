package org.thingsboard.server.transport.lwm2m.secure.credentials;

import lombok.Data;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;

@Data
public class LwM2MCredentials {
    private LwM2MClientCredentialsConfig client;
    private LwM2MBootstrapConfig bootstrap;
}
