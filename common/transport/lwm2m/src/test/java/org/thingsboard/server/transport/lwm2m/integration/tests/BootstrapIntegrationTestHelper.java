/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class BootstrapIntegrationTestHelper extends SecureIntegrationTestHelper {

    public LeshanBootstrapServer bootstrapServer;
    public final PublicKey bootstrapServerPublicKey;
    public final PrivateKey bootstrapServerPrivateKey;

    public BootstrapIntegrationTestHelper() {
        super();

        // create bootstrap server credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fb136894878a9696d45fdb04506b9eb49ddcfba71e4e1b4ce23d5c3ac382d6b4".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("3deed825e808f8ed6a9a74ff6bd24e3d34b1c0c5fc253422f7febadbdc9cb9e6".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("35a8303e67a7e99d06552a0f8f6c8f1bf91a174396f4fad6211ae227e890da11".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            bootstrapServerPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            bootstrapServerPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore, BootstrapConfigStore bootstrapStore) {
        if (bootstrapStore == null) {
            bootstrapStore = unsecuredBootstrapStore();
        }

        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }

        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setConfigStore(bootstrapStore);
        builder.setSecurityStore(securityStore);
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setPrivateKey(bootstrapServerPrivateKey);
        builder.setPublicKey(bootstrapServerPublicKey);

        bootstrapServer = builder.build();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore) {
        createBootstrapServer(securityStore, null);
    }

    public Security withoutSecurity() {
        // Create Security Object (with bootstrap server only)
        String bsUrl = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                + bootstrapServer.getUnsecuredAddress().getPort();
        return new Security(bsUrl, true, 3, new byte[0], new byte[0], new byte[0], 12345);
    }

    @Override
    public void createClient() {
        createClient(withoutSecurity(), null);
    }

    public void createPSKClient(String pskIdentity, byte[] pskKey) {
        // Create Security Object (with bootstrap server only)
        String bsUrl = "coaps://" + bootstrapServer.getSecuredAddress().getHostString() + ":"
                + bootstrapServer.getSecuredAddress().getPort();
        byte[] pskId = pskIdentity.getBytes(StandardCharsets.UTF_8);
        Security security = Security.pskBootstrap(bsUrl, pskId, pskKey);

        createClient(security, null);
    }

    @Override
    public void createRPKClient() {
        String bsUrl = "coaps://" + bootstrapServer.getSecuredAddress().getHostString() + ":"
                + bootstrapServer.getSecuredAddress().getPort();
        Security security = Security.rpkBootstrap(bsUrl, clientPublicKey.getEncoded(), clientPrivateKey.getEncoded(),
                bootstrapServerPublicKey.getEncoded());

        createClient(security, null);
    }

    public void createClient(Security security, ObjectsInitializer initializer) {
        if (initializer == null) {
            initializer = new ObjectsInitializer();
        }

        // Initialize LWM2M Object Tree
        initializer.setInstancesForObject(LwM2mId.SECURITY, security);
        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new Device("Eclipse Leshan", IntegrationTestHelper.MODEL_NUMBER, "12345", "U"));
        initializer.setClassForObject(LwM2mId.SERVER, DummyInstanceEnabler.class);
        createClient(initializer);
    }

    public void createClient(ObjectsInitializer initializer) {
        // Create Leshan Client
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setObjects(initializer.createAll());
        client = builder.build();
        setupClientMonitoring();
    }

    public BootstrapSecurityStore bsSecurityStore(final SecurityMode mode) {

        return new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String identity) {
                if (mode == SecurityMode.PSK) {
                    if (org.thingsboard.server.transport.lwm2m.integration.tests.BootstrapIntegrationTestHelper.GOOD_PSK_ID.equals(identity)) {
                        return pskSecurityInfo();
                    }
                }
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                if (getCurrentEndpoint().equals(endpoint)) {
                    SecurityInfo info;
                    if (mode == SecurityMode.PSK) {
                        info = pskSecurityInfo();
                        return Arrays.asList(info);
                    } else if (mode == SecurityMode.RPK) {
                        info = rpkSecurityInfo();
                        return Arrays.asList(info);
                    }
                }
                return Arrays.asList();
            }
        };
    }

    public SecurityInfo pskSecurityInfo() {
        SecurityInfo info = SecurityInfo.newPreSharedKeyInfo(getCurrentEndpoint(),
                org.thingsboard.server.transport.lwm2m.integration.tests.BootstrapIntegrationTestHelper.GOOD_PSK_ID, org.thingsboard.server.transport.lwm2m.integration.tests.BootstrapIntegrationTestHelper.GOOD_PSK_KEY);
        return info;
    }

    public SecurityInfo rpkSecurityInfo() {
        SecurityInfo info = SecurityInfo.newRawPublicKeyInfo(getCurrentEndpoint(), clientPublicKey);
        return info;
    }

    private BootstrapSecurityStore dummyBsSecurityStore() {
        return new BootstrapSecurityStore() {

            @Override
            public SecurityInfo getByIdentity(String identity) {
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        };
    }

    public BootstrapConfigStore unsecuredBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.serverId = 1111;
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coap://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getUnsecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore deleteSecurityStore(Integer... objectToDelete) {
        String[] pathToDelete = new String[objectToDelete.length];
        for (int i = 0; i < pathToDelete.length; i++) {
            pathToDelete[i] = "/" + objectToDelete[i];

        }
        return deleteSecurityStore(pathToDelete);
    }

    public BootstrapConfigStore deleteSecurityStore(final String... pathToDelete) {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();
                bsConfig.toDelete = Arrays.asList(pathToDelete);
                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore unsecuredWithAclBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.serverId = 1111;
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coap://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getUnsecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                // ACL
                ACLConfig aclConfig = new ACLConfig();
                aclConfig.objectId = 3;
                aclConfig.objectInstanceId = 0;
                HashMap<Integer, Long> acl = new HashMap<Integer, Long>();
                acl.put(3333, 1l); // server with short id 3333 has just read(1) right on device object (3/0)
                aclConfig.acls = acl;
                aclConfig.AccessControlOwner = 2222;
                bsConfig.acls.put(0, aclConfig);

                aclConfig = new ACLConfig();
                aclConfig.objectId = 4;
                aclConfig.objectInstanceId = 0;
                aclConfig.AccessControlOwner = 2222;
                bsConfig.acls.put(1, aclConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore pskBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.serverId = 1111;
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coaps://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getSecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.PSK;
                dmSecurity.publicKeyOrId = GOOD_PSK_ID.getBytes();
                dmSecurity.secretKey = GOOD_PSK_KEY;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore rpkBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.serverId = 1111;
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coaps://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getSecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.RPK;
                dmSecurity.publicKeyOrId = clientPublicKey.getEncoded();
                dmSecurity.secretKey = clientPrivateKey.getEncoded();
                dmSecurity.serverPublicKey = serverPublicKey.getEncoded();
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    @Override
    public void dispose() {
        super.dispose();
        ((EditableSecurityStore) server.getSecurityStore()).remove(getCurrentEndpoint(), false);
    }
}
