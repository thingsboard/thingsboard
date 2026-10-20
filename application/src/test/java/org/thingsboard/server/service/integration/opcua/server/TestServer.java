// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.opcua.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_X509;

@Slf4j
public class TestServer {

    private static final int TCP_BIND_PORT = 12686;
    private static final int HTTPS_BIND_PORT = 28443;

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());

        try {
            NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private OpcUaServer server;
    private TestNamespace exampleNamespace;
    private final OpcUaServerConfig serverConfig;

    @Getter
    private Boolean started = Boolean.FALSE;

    public TestServer() {
        try {
            Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "server", "security");
            Files.createDirectories(securityTempDir);
            if (!Files.exists(securityTempDir)) {
                throw new Exception("unable to create security temp dir: " + securityTempDir);
            }

            File pkiDir = securityTempDir.resolve("pki").toFile();

            log.info("security dir: {}", securityTempDir.toAbsolutePath());
            log.info("security pki dir: {}", pkiDir.getAbsolutePath());

            KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

            DefaultCertificateManager certificateManager = new DefaultCertificateManager(
                    loader.getServerKeyPair(),
                    loader.getServerCertificateChain()
            );

            DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);

            DefaultServerCertificateValidator certificateValidator =
                    new DefaultServerCertificateValidator(trustListManager);

            KeyPair httpsKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

            SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new SelfSignedHttpsCertificateBuilder(httpsKeyPair);
            httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
            HostnameUtil.getHostnames("0.0.0.0").forEach(httpsCertificateBuilder::addDnsName);
            X509Certificate httpsCertificate = httpsCertificateBuilder.build();

            UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                    true,
                    authChallenge -> {
                        String username = authChallenge.getUsername();
                        String password = authChallenge.getPassword();

                        boolean userOk = "user".equals(username) && "password1".equals(password);
                        boolean adminOk = "admin".equals(username) && "password2".equals(password);

                        return userOk || adminOk;
                    }
            );

            X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

            // If you need to use multiple certificates you'll have to be smarter than this.
            X509Certificate certificate = certificateManager.getCertificates()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

            // The configured application URI must match the one in the certificate(s)
            String applicationUri = CertificateUtil
                    .getSanUri(certificate)
                    .orElseThrow(() -> new UaRuntimeException(
                            StatusCodes.Bad_ConfigurationError,
                            "certificate is missing the application URI"));

            Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate);

            serverConfig = OpcUaServerConfig.builder()
                    .setApplicationUri(applicationUri)
                    .setApplicationName(LocalizedText.english("Eclipse Milo OPC UA Example Server"))
                    .setEndpoints(endpointConfigurations)
                    .setBuildInfo(
                            new BuildInfo(
                                    "urn:eclipse:milo:example-server",
                                    "eclipse",
                                    "eclipse milo example server",
                                    OpcUaServer.SDK_VERSION,
                                    "", DateTime.now()))
                    .setCertificateManager(certificateManager)
                    .setTrustListManager(trustListManager)
                    .setCertificateValidator(certificateValidator)
                    .setHttpsKeyPair(httpsKeyPair)
                    .setHttpsCertificate(httpsCertificate)
                    .setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
                    .setProductUri("urn:eclipse:milo:example-server")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate) {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");

        Set<String> hostnames = new LinkedHashSet<>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0"));

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                        .setBindAddress(bindAddress)
                        .setHostname(hostname)
                        .setPath("")
                        .setCertificate(certificate)
                        .addTokenPolicies(
                                USER_TOKEN_POLICY_ANONYMOUS,
                                USER_TOKEN_POLICY_USERNAME,
                                USER_TOKEN_POLICY_X509);


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));
                endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));

                EndpointConfiguration.Builder discoveryBuilder = builder.copy()
                        .setPath("/discovery")
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
                endpointConfigurations.add(buildHttpsEndpoint(discoveryBuilder));
            }
        }

        return endpointConfigurations;
    }

    private static EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindPort(TCP_BIND_PORT)
                .build();
    }

    private static EndpointConfiguration buildHttpsEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
                .setTransportProfile(TransportProfile.HTTPS_UABINARY)
                .setBindPort(HTTPS_BIND_PORT)
                .build();
    }

    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() throws ExecutionException, InterruptedException {
        if (started) {
            server.shutdown().get();
        }
        server = new OpcUaServer(serverConfig);

        exampleNamespace = new TestNamespace(server);
        exampleNamespace.startup();
        started = Boolean.TRUE;
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        exampleNamespace.shutdown();
        started = Boolean.FALSE;
        return server.shutdown();
    }

}
