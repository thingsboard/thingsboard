/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m;

import static org.eclipse.leshan.client.object.Security.*;
import static org.eclipse.leshan.core.LwM2mId.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.ClientHandshaker;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingClientHandshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.californium.DefaultEndpointFactory;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.thingsboard.server.transport.lwm2m.client.object.LwM2MDevice;
import org.thingsboard.server.transport.lwm2m.client.object.LwM2MLocation;
import org.thingsboard.server.transport.lwm2m.client.object.RandomTemperatureSensor;
import org.thingsboard.server.transport.lwm2m.utils.Util;

@Slf4j
public class LwM2mDeviceEmulator {
    private final static String[] modelPaths = Util.modelPaths;
    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;
    private final static String DEFAULT_ENDPOINT = "LeshanClientDemo";
    private final static int DEFAULT_LIFETIME = 5 * 60; // 5min in seconds
    private final static String USAGE = "java -jar leshan-client-demo.jar [OPTION]\n\n";

    private static LwM2MLocation locationInstance;

    public static void main(final String[] args) {
        startClient ( args);
    }

    public static void startClient(final String[] args) {
        // Define options for command line tools
        Options options = new Options();

        final StringBuilder PSKChapter = new StringBuilder();
        PSKChapter.append("\n .");
        PSKChapter.append("\n .");
        PSKChapter.append("\n ================================[ PSK ]=================================");
        PSKChapter.append("\n | By default Leshan demo use non secure connection.                    |");
        PSKChapter.append("\n | To use PSK, -i and -p options should be used together.               |");
        PSKChapter.append("\n ------------------------------------------------------------------------");

        final StringBuilder RPKChapter = new StringBuilder();
        RPKChapter.append("\n .");
        RPKChapter.append("\n .");
        RPKChapter.append("\n ================================[ RPK ]=================================");
        RPKChapter.append("\n | By default Leshan demo use non secure connection.                    |");
        RPKChapter.append("\n | To use RPK, -cpubk -cprik -spubk options should be used together.    |");
        RPKChapter.append("\n | To get helps about files format and how to generate it, see :        |");
        RPKChapter.append("\n | See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        RPKChapter.append("\n ------------------------------------------------------------------------");

        final StringBuilder X509Chapter = new StringBuilder();
        X509Chapter.append("\n .");
        X509Chapter.append("\n .");
        X509Chapter.append("\n ================================[X509]==================================");
        X509Chapter.append("\n | By default Leshan demo use non secure connection.                    |");
        X509Chapter.append("\n | To use X509, -ccert -cprik -scert options should be used together.   |");
        X509Chapter.append("\n | To get helps about files format and how to generate it, see :        |");
        X509Chapter.append("\n | See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509Chapter.append("\n ------------------------------------------------------------------------");

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("n", true, String.format(
                "Set the endpoint name of the Client.\nDefault: the local hostname or '%s' if any.", DEFAULT_ENDPOINT));
        options.addOption("b", false, "If present use bootstrap.");
        options.addOption("l", true, String.format(
                "The lifetime in seconds used to register, ignored if -b is used.\n Default : %ds", DEFAULT_LIFETIME));
        options.addOption("cp", true,
                "The communication period in seconds which should be smaller than the lifetime, will be used even if -b is used.");
        options.addOption("lh", true, "Set the local CoAP address of the Client.\n  Default: any local address.");
        options.addOption("lp", true,
                "Set the local CoAP port of the Client.\n  Default: A valid port value is between 0 and 65535.");
        options.addOption("u", true, String.format("Set the LWM2M or Bootstrap server URL.\nDefault: localhost:%d.",
                LwM2m.DEFAULT_COAP_PORT));
        options.addOption("r", false, "Force reconnect/rehandshake on update.");
        options.addOption("f", false, "Do not try to resume session always, do a full handshake.");
        options.addOption("ocf",
                "activate support of old/unofficial content format .\n See https://github.com/eclipse/leshan/pull/720");
        options.addOption("oc", "activate support of old/deprecated cipher suites.");
        Builder aa = Option.builder("aa");
        aa.desc("Use additional attributes at registration time, syntax is \n -aa attrName1=attrValue1 attrName2=\\\"attrValue2\\\" ...");
        aa.hasArgs();
        options.addOption(aa.build());
        options.addOption("m", true, "A folder which contains object models in OMA DDF(.xml)format.");
        options.addOption("pos", true,
                "Set the initial location (latitude, longitude) of the device to be reported by the Location object.\n Format: lat_float:long_float");
        options.addOption("sf", true, "Scale factor to apply when shifting position.\n Default is 1.0.");
        options.addOption("al", true, "Set the initial location (altitude) of the device to be reported by the Location object.\n Default is 0.");
        options.addOption("sp", true, "Set the initial location (speed) of the device to be reported by the Location object.\n Default is 0.\n Format: float m/s" + PSKChapter);
        options.addOption("i", true, "Set the LWM2M or Bootstrap server PSK identity in ascii.");
        options.addOption("p", true, "Set the LWM2M or Bootstrap server Pre-Shared-Key in hexa." + RPKChapter);
        options.addOption("cpubk", true,
                "The path to your client public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding).");
        options.addOption("cprik", true,
                "The path to your client private key file.\nThe private key should be in PKCS#8 format (DER encoding).");
        options.addOption("spubk", true,
                "The path to your server public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding)."
                        + X509Chapter);
        options.addOption("ccert", true,
                "The path to your client certificate file.\n The certificate Common Name (CN) should generaly be equal to the client endpoint name (see -n option).\nThe certificate should be in X509v3 format (DER encoding).");
        options.addOption("scert", true,
                "The path to your server certificate file.\n The certificate should be in X509v3 format (DER encoding).");

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(90);
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected optionsLwM2M
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if PSK config is not complete
        if ((cl.hasOption("i") && !cl.hasOption("p")) || !cl.hasOption("i") && cl.hasOption("p")) {
            System.err
                    .println("You should precise identity (-i) and Pre-Shared-Key (-p) if you want to connect in PSK");
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if all RPK config is not complete
        boolean rpkConfig = false;
        if (cl.hasOption("cpubk") || cl.hasOption("spubk")) {
            if (!cl.hasOption("cpubk") || !cl.hasOption("cprik") || !cl.hasOption("spubk")) {
                System.err.println("cpubk, cprik and spubk should be used together to connect using RPK");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                rpkConfig = true;
            }
        }

        // Abort if all X509 config is not complete
        boolean x509config = false;
        if (cl.hasOption("ccert") || cl.hasOption("scert")) {
            if (!cl.hasOption("ccert") || !cl.hasOption("cprik") || !cl.hasOption("scert")) {
                System.err.println("ccert, cprik and scert should be used together to connect using X509");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                x509config = true;
            }
        }

        // Abort if cprik is used without complete RPK or X509 config
        if (cl.hasOption("cprik")) {
            if (!x509config && !rpkConfig) {
                System.err.println(
                        "cprik should be used with ccert and scert for X509 config OR cpubk and spubk for RPK config");
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // Get endpoint name
        String endpoint;
        if (cl.hasOption("n")) {
            endpoint = cl.getOptionValue("n");
        } else {
            try {
                endpoint = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                endpoint = DEFAULT_ENDPOINT;
            }
        }

        // Get lifetime
        int lifetime;
        if (cl.hasOption("l")) {
            lifetime = Integer.parseInt(cl.getOptionValue("l"));
        } else {
            lifetime = DEFAULT_LIFETIME;
        }

        // Get lifetime
        Integer communicationPeriod = null;
        if (cl.hasOption("cp")) {
            communicationPeriod = Integer.valueOf(cl.getOptionValue("cp")) * 1000;
        }

        // Get additional attributes
        Map<String, String> additionalAttributes = null;
        if (cl.hasOption("aa")) {
            additionalAttributes = new HashMap<>();
            Pattern p1 = Pattern.compile("(.*)=\"(.*)\"");
            Pattern p2 = Pattern.compile("(.*)=(.*)");
            String[] values = cl.getOptionValues("aa");
            for (String v : values) {
                Matcher m = p1.matcher(v);
                if (m.matches()) {
                    String attrName = m.group(1);
                    String attrValue = m.group(2);
                    additionalAttributes.put(attrName, attrValue);
                } else {
                    m = p2.matcher(v);
                    if (m.matches()) {
                        String attrName = m.group(1);
                        String attrValue = m.group(2);
                        additionalAttributes.put(attrName, attrValue);
                    } else {
                        System.err.println(String.format("Invalid syntax for additional attributes : %s", v));
                        return;
                    }
                }
            }
        }

        // Get server URI
        String serverURI;
        if (cl.hasOption("u")) {
            if (cl.hasOption("i") || cl.hasOption("cpubk"))
                serverURI = "coaps://" + cl.getOptionValue("u");
            else
                serverURI = "coap://" + cl.getOptionValue("u");
        } else {
            if (cl.hasOption("i") || cl.hasOption("cpubk") || cl.hasOption("ccert"))
                serverURI = "coaps://localhost:" + (LwM2m.DEFAULT_COAP_SECURE_PORT);
            else
                serverURI = "coap://localhost:" + (LwM2m.DEFAULT_COAP_PORT);
        }

        // get PSK info
        byte[] pskIdentity = null;
        byte[] pskKey = null;
        if (cl.hasOption("i")) {
            pskIdentity = cl.getOptionValue("i").getBytes();
            pskKey = Hex.decodeHex(cl.getOptionValue("p").toCharArray());
        }

        // get RPK info
        PublicKey clientPublicKey = null;
        PrivateKey clientPrivateKey = null;
        PublicKey serverPublicKey = null;
        if (cl.hasOption("cpubk")) {
            try {
                clientPrivateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("cprik"));
                clientPublicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("cpubk"));
                serverPublicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("spubk"));
            } catch (Exception e) {
                System.err.println("Unable to load RPK files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get X509 info
        X509Certificate clientCertificate = null;
        X509Certificate serverCertificate = null;
        if (cl.hasOption("ccert")) {
            try {
                clientPrivateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("cprik"));
                clientCertificate = SecurityUtil.certificate.readFromFile(cl.getOptionValue("ccert"));
                serverCertificate = SecurityUtil.certificate.readFromFile(cl.getOptionValue("scert"));
            } catch (Exception e) {
                System.err.println("Unable to load X509 files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get local address
        String localAddress = null;
        int localPort = 0;
        if (cl.hasOption("lh")) {
            localAddress = cl.getOptionValue("lh");
        }
        if (cl.hasOption("lp")) {
            localPort = Integer.parseInt(cl.getOptionValue("lp"));
        }

        Float latitude = null;
        Float longitude = null;
        Float scaleFactor = 1.0f;
        Float altitude = 0f;
        Float speed = 0f;
        // get initial Location
        if (cl.hasOption("pos")) {
            try {
                String pos = cl.getOptionValue("pos");
                int colon = pos.indexOf(':');
                if (colon == -1 || colon == 0 || colon == pos.length() - 1) {
                    System.err.println("Position must be a set of two floats separated by a colon, e.g. 48.131:11.459");
                    formatter.printHelp(USAGE, options);
                    return;
                }
                latitude = Float.valueOf(pos.substring(0, colon));
                longitude = Float.valueOf(pos.substring(colon + 1));
            } catch (NumberFormatException e) {
                System.err.println("Position must be a set of two floats separated by a colon, e.g. 48.131:11.459");
                formatter.printHelp(USAGE, options);
                return;
            }
        }
        if (cl.hasOption("sf")) {
            try {
                scaleFactor = Float.valueOf(cl.getOptionValue("sf"));
            } catch (NumberFormatException e) {
                System.err.println("Scale factor must be a float, e.g. 1.0 or 0.01");
                formatter.printHelp(USAGE, options);
                return;
            }
        }
        if (cl.hasOption("al")) {
            try {
                altitude = Float.valueOf(cl.getOptionValue("al"));
            } catch (NumberFormatException e) {
                System.err.println("Altitude must be a float, e.g. 186.0 or -186.01");
                formatter.printHelp(USAGE, options);
                return;
            }
        }
        if (cl.hasOption("sp")) {
            try {
                speed = Float.valueOf(cl.getOptionValue("sp"));
            } catch (NumberFormatException e) {
                System.err.println("Speed must be a float (m/s), e.g. 186.0 or 0.01");
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        try {
            createAndStartClient(endpoint, localAddress, localPort, cl.hasOption("b"), additionalAttributes, lifetime,
                    communicationPeriod, serverURI, pskIdentity, pskKey, clientPrivateKey, clientPublicKey,
                    serverPublicKey, clientCertificate, serverCertificate, latitude, longitude, scaleFactor, altitude, speed,
                    cl.hasOption("ocf"), cl.hasOption("oc"), cl.hasOption("r"), cl.hasOption("f"), modelsFolderPath);
        } catch (Exception e) {
            System.err.println("Unable to create and start client ...");
            e.printStackTrace();
            return;
        }
    }

    public static void createAndStartClient(String endpoint, String localAddress, int localPort, boolean needBootstrap,
                                            Map<String, String> additionalAttributes, int lifetime, Integer communicationPeriod, String serverURI,
                                            byte[] pskIdentity, byte[] pskKey, PrivateKey clientPrivateKey, PublicKey clientPublicKey,
                                            PublicKey serverPublicKey, X509Certificate clientCertificate, X509Certificate serverCertificate,
                                            Float latitude, Float longitude, float scaleFactor, float altitude, float speed, boolean supportOldFormat,
                                            boolean supportDeprecatedCiphers, boolean reconnectOnUpdate, boolean forceFullhandshake,
                                            String modelsFolderPath) throws CertificateEncodingException {

        locationInstance = new LwM2MLocation(latitude, longitude, scaleFactor, altitude, speed);

        // Initialize model
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }

        // Initialize object list
        final LwM2mModel model = new StaticModel(models);
        final ObjectsInitializer initializer = new ObjectsInitializer(model);
        if (needBootstrap) {
            if (pskIdentity != null) {
                initializer.setInstancesForObject(SECURITY, pskBootstrap(serverURI, pskIdentity, pskKey));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (clientPublicKey != null) {
                initializer.setInstancesForObject(SECURITY, rpkBootstrap(serverURI, clientPublicKey.getEncoded(),
                        clientPrivateKey.getEncoded(), serverPublicKey.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (clientCertificate != null) {
                initializer.setInstancesForObject(SECURITY, x509Bootstrap(serverURI, clientCertificate.getEncoded(),
                        clientPrivateKey.getEncoded(), serverCertificate.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else {
                initializer.setInstancesForObject(SECURITY, noSecBootstap(serverURI));
                initializer.setClassForObject(SERVER, Server.class);
            }
        } else {
            if (pskIdentity != null) {
                initializer.setInstancesForObject(SECURITY, psk(serverURI, 123, pskIdentity, pskKey));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            } else if (clientPublicKey != null) {
                initializer.setInstancesForObject(SECURITY, rpk(serverURI, 123, clientPublicKey.getEncoded(),
                        clientPrivateKey.getEncoded(), serverPublicKey.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            } else if (clientCertificate != null) {
                initializer.setInstancesForObject(SECURITY, x509(serverURI, 123, clientCertificate.getEncoded(),
                        clientPrivateKey.getEncoded(), serverCertificate.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            } else {
                initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            }
        }
        initializer.setInstancesForObject(DEVICE, new LwM2MDevice());
        initializer.setInstancesForObject(LOCATION, locationInstance);
        initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());
        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanClientBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!supportDeprecatedCiphers);

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setCommunicationPeriod(communicationPeriod);
        engineFactory.setReconnectOnUpdate(reconnectOnUpdate);
        engineFactory.setResumeOnConnect(!forceFullhandshake);

        // configure EndpointFactory
        DefaultEndpointFactory endpointFactory = new DefaultEndpointFactory("LWM2M CLIENT") {
            @Override
            protected Connector createSecuredConnector(DtlsConnectorConfig dtlsConfig) {

                return new DTLSConnector(dtlsConfig) {
                    @Override
                    protected void onInitializeHandshaker(Handshaker handshaker) {
                        handshaker.addSessionListener(new SessionAdapter() {

                            @Override
                            public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
                                if (handshaker instanceof ServerHandshaker) {
                                    log.info("DTLS Full Handshake initiated by server : STARTED ...");
                                } else if (handshaker instanceof ResumingServerHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by server : STARTED ...");
                                } else if (handshaker instanceof ClientHandshaker) {
                                    log.info("DTLS Full Handshake initiated by client : STARTED ...");
                                } else if (handshaker instanceof ResumingClientHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by client : STARTED ...");
                                }
                            }

                            @Override
                            public void sessionEstablished(Handshaker handshaker, DTLSSession establishedSession)
                                    throws HandshakeException {
                                if (handshaker instanceof ServerHandshaker) {
                                    log.info("DTLS Full Handshake initiated by server : SUCCEED");
                                } else if (handshaker instanceof ResumingServerHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by server : SUCCEED");
                                } else if (handshaker instanceof ClientHandshaker) {
                                    log.info("DTLS Full Handshake initiated by client : SUCCEED");
                                } else if (handshaker instanceof ResumingClientHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by client : SUCCEED");
                                }
                            }

                            @Override
                            public void handshakeFailed(Handshaker handshaker, Throwable error) {
                                // get cause
                                String cause;
                                if (error != null) {
                                    if (error.getMessage() != null) {
                                        cause = error.getMessage();
                                    } else {
                                        cause = error.getClass().getName();
                                    }
                                } else {
                                    cause = "unknown cause";
                                }

                                if (handshaker instanceof ServerHandshaker) {
                                    log.info("DTLS Full Handshake initiated by server : FAILED ({})", cause);
                                } else if (handshaker instanceof ResumingServerHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by server : FAILED ({})", cause);
                                } else if (handshaker instanceof ClientHandshaker) {
                                    log.info("DTLS Full Handshake initiated by client : FAILED ({})", cause);
                                } else if (handshaker instanceof ResumingClientHandshaker) {
                                    log.info("DTLS abbreviated Handshake initiated by client : FAILED ({})", cause);
                                }
                            }
                        });
                    }
                };
            }
        };

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setLocalAddress(localAddress, localPort);
        builder.setObjects(enablers);
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
        if (supportOldFormat) {
            builder.setDecoder(new DefaultLwM2mNodeDecoder(true));
            builder.setEncoder(new DefaultLwM2mNodeEncoder(true));
        }
        builder.setAdditionalAttributes(additionalAttributes);
        final LeshanClient client = builder.build();

        client.getObjectTree().addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                log.info("Object {} disabled.", object.getId());
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                log.info("Object {} enabled.", object.getId());
            }
        });

        // Display client public key to easily add it in demo servers.
        if (clientPublicKey != null) {
            PublicKey rawPublicKey = clientPublicKey;
            if (rawPublicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);

                // Get Curves params
                String params = ecPublicKey.getParams().toString();

                log.info(
                        "Client uses RPK : \n Elliptic Curve parameters  : {} \n Public x coord : {} \n Public y coord : {} \n Public Key (Hex): {} \n Private Key (Hex): {}",
                        params, Hex.encodeHexString(x), Hex.encodeHexString(y),
                        Hex.encodeHexString(rawPublicKey.getEncoded()),
                        Hex.encodeHexString(clientPrivateKey.getEncoded()));

            } else {
                throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");
            }
        }
        // Display X509 credentials to easily at it in demo servers.
        if (clientCertificate != null) {
            log.info("Client uses X509 : \n X509 Certificate (Hex): {} \n Private Key (Hex): {}",
                    Hex.encodeHexString(clientCertificate.getEncoded()),
                    Hex.encodeHexString(clientPrivateKey.getEncoded()));
        }

        // Print commands help
        StringBuilder commandsHelp = new StringBuilder("Commands available :");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - create <objectId> : to enable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - delete <objectId> : to disable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - update : to trigger a registration update.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - w : to move to North.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - a : to move to East.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - s : to move to South.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - d : to move to West.");
        commandsHelp.append(System.lineSeparator());
        log.info(commandsHelp.toString());

        // Start the client
        client.start();

        // De-register on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                client.destroy(true); // send de-registration request before destroy
            }
        });

        // Change the location through the Console
        try (Scanner scanner = new Scanner(System.in)) {
            List<Character> wasdCommands = Arrays.asList('w', 'a', 's', 'd');
            while (scanner.hasNext()) {
                String command = scanner.next();
                if (command.startsWith("create")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (client.getObjectTree().getObjectEnabler(objectId) != null) {
                            log.info("Object {} already enabled.", objectId);
                        }
                        if (model.getObjectModel(objectId) == null) {
                            log.info("Unable to enable Object {} : there no model for this.", objectId);
                        } else {
                            ObjectsInitializer objectsInitializer = new ObjectsInitializer(model);
                            objectsInitializer.setDummyInstancesForObject(objectId);
                            LwM2mObjectEnabler object = objectsInitializer.create(objectId);
                            client.getObjectTree().addObjectEnabler(object);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        log.info("Invalid syntax, <objectid> must be an integer : create <objectId>");
                    }
                } else if (command.startsWith("delete")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (objectId == 0 || objectId == 0 || objectId == 3) {
                            log.info("Object {} can not be disabled.", objectId);
                        } else if (client.getObjectTree().getObjectEnabler(objectId) == null) {
                            log.info("Object {} is not enabled.", objectId);
                        } else {
                            client.getObjectTree().removeObjectEnabler(objectId);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        log.info("\"Invalid syntax, <objectid> must be an integer : delete <objectId>");
                    }
                } else if (command.startsWith("update")) {
                    client.triggerRegistrationUpdate();
                } else if (command.length() == 1 && wasdCommands.contains(command.charAt(0))) {
                    locationInstance.moveLocation(command);
                } else {
                    log.info("Unknown command '{}'", command);
                }
            }
        }
    }
}
