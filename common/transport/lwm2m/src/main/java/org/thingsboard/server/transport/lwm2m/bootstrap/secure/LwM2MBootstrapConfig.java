package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

import java.util.ArrayList;
import java.util.List;

@Data
public class LwM2MBootstrapConfig extends BootstrapConfig {


    public LwM2MBootstrapConfig(String uri0, int securityMode0, String publicKeyOrId0, String serverPublicKey0, String secretKey0,
                                String uri1, int securityMode1, String publicKeyOrId1, String serverPublicKey1, String secretKey1)  {
        /** Delete old security objects */
        this.toDelete.add("0");
        this.toDelete.add("1");
        /** Server Configuration (object 1) as defined in LWM2M 1.0.x TS. */
        ServerConfig server0 = new ServerConfig();
        server0.shortId = 123;
        server0.lifetime = 300;
        server0.defaultMinPeriod = 1;
        server0.notifIfDisabled = true;
        server0.binding = BindingMode.U;
        servers.put(0, server0);
        /** Security Configuration (object 0) as defined in LWM2M 1.0.x TS. */
        ServerSecurity serverSecurity0 = new ServerSecurity();
        serverSecurity0.uri = uri0;
        serverSecurity0.bootstrapServer =  true;
        serverSecurity0.securityMode =  SecurityMode.fromCode(securityMode0);
        serverSecurity0.publicKeyOrId = (publicKeyOrId0 != null && !publicKeyOrId0.isEmpty()) ? Hex.decodeHex(publicKeyOrId0.toCharArray())  : new byte[] {};
        serverSecurity0.serverPublicKey = (serverPublicKey0 != null && !serverPublicKey0.isEmpty()) ? Hex.decodeHex(serverPublicKey0.toCharArray())  :new byte[] {};
        serverSecurity0.secretKey = (secretKey0 != null && !secretKey0.isEmpty()) ? Hex.decodeHex(secretKey0.toCharArray())  : new byte[] {};
        serverSecurity0.serverId =  111;

        ServerSecurity serverSecurity1 = new ServerSecurity();
        serverSecurity1.uri = uri1;
        serverSecurity1.bootstrapServer =  false;
        serverSecurity1.securityMode =  SecurityMode.fromCode(securityMode1);
        serverSecurity1.publicKeyOrId = (publicKeyOrId1 != null && !publicKeyOrId1.isEmpty()) ? Hex.decodeHex(publicKeyOrId1.toCharArray())  : new byte[] {};
        serverSecurity1.serverPublicKey = (serverPublicKey1 != null && !serverPublicKey1.isEmpty()) ? Hex.decodeHex(serverPublicKey1.toCharArray())  :new byte[] {};
        serverSecurity1.secretKey = (secretKey1 != null && !secretKey1.isEmpty()) ? Hex.decodeHex(secretKey1.toCharArray())  : new byte[] {};
        serverSecurity1.serverId =  123;

        security.put(0, serverSecurity0);
        security.put(1, serverSecurity1);


    }

}
