// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.server.uplink;

import com.google.common.util.concurrent.MoreExecutors;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.X509CertificateRevokedEvent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mClientCertificateRegistry;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mClientCertificateRegistry.ClientCertificate;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mSecurityStore;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLwM2mUplinkMsgHandlerRevocationTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final String ENDPOINT = "endpoint";
    private static final String REVOKED_HASH = "revoked";
    private static final String REGISTRATION_ID = "registration";

    private final TbLwM2MDtlsSessionStore sessionStore = mock(TbLwM2MDtlsSessionStore.class);
    private final LwM2mClientContext clientContext = mock(LwM2mClientContext.class);
    private final RegistrationStore registrationStore = mock(RegistrationStore.class);
    private final TbLwM2mSecurityStore securityStore = mock(TbLwM2mSecurityStore.class);
    private final LwM2mClientCertificateRegistry certificateRegistry = new LwM2mClientCertificateRegistry();
    private final Registration registration = registration(new Date(1000));
    private DefaultLwM2mUplinkMsgHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultLwM2mUplinkMsgHandler(mock(TransportService.class), mock(LwM2mTransportContext.class),
                mock(LwM2MAttributesService.class), mock(LwM2MSessionManager.class), mock(LwM2MOtaUpdateService.class),
                mock(LwM2MTransportServerConfig.class), mock(LwM2MTelemetryLogService.class), mock(LwM2mTransportServerHelper.class),
                sessionStore, clientContext, mock(LwM2mDownlinkMsgHandler.class), mock(LwM2mVersionedModelProvider.class),
                registrationStore, securityStore, mock(LwM2MModelConfigService.class), certificateRegistry);
        ReflectionTestUtils.setField(handler, "executor", MoreExecutors.newDirectExecutorService());
        ConnectionId cid = new ConnectionId(new byte[]{1});
        certificateRegistry.registerPending(cid, new ClientCertificate(ENDPOINT, TENANT_ID, REVOKED_HASH, Long.MAX_VALUE), 0);
        certificateRegistry.commitPending(cid);
    }

    @Test
    void givenRegistrationServedByThisNode_whenCertificateRevoked_thenClientIsUnregistered() throws Exception {
        LwM2mClient client = registeredClient();
        Registration current = registration(new Date(1000));
        when(registrationStore.getRegistration(REGISTRATION_ID)).thenReturn(current);

        handler.onX509CertificateRevoked(new X509CertificateRevokedEvent(TENANT_ID, REVOKED_HASH));

        verify(clientContext).unregister(client, registration);
        verify(securityStore).remove(ENDPOINT, REGISTRATION_ID);
        verify(registrationStore).removeRegistration(REGISTRATION_ID);
    }

    @Test
    void givenRegistrationUpdatedThroughAnotherNode_whenCertificateRevoked_thenSharedStateIsKept() throws Exception {
        registeredClient();
        Registration updatedThroughAnotherNode = registration(new Date(2000));
        when(registrationStore.getRegistration(REGISTRATION_ID)).thenReturn(updatedThroughAnotherNode);

        handler.onX509CertificateRevoked(new X509CertificateRevokedEvent(TENANT_ID, REVOKED_HASH));

        verifySharedStateIsKept();
    }

    @Test
    void givenEndpointRegisteredAgainOnAnotherNode_whenCertificateRevoked_thenSharedStateIsKept() throws Exception {
        registeredClient();
        // Registering again replaces the registration, its old id is not found any more
        when(registrationStore.getRegistration(REGISTRATION_ID)).thenReturn(null);

        handler.onX509CertificateRevoked(new X509CertificateRevokedEvent(TENANT_ID, REVOKED_HASH));

        verifySharedStateIsKept();
    }

    @Test
    void givenNoClientOnThisNode_whenCertificateRevoked_thenSessionStoreIsKept() throws Exception {
        when(clientContext.getLwM2mClients()).thenReturn(List.of());

        handler.onX509CertificateRevoked(new X509CertificateRevokedEvent(TENANT_ID, REVOKED_HASH));

        verifySharedStateIsKept();
    }

    private LwM2mClient registeredClient() {
        LwM2mClient client = mock(LwM2mClient.class);
        when(client.getEndpoint()).thenReturn(ENDPOINT);
        when(client.getRegistration()).thenReturn(registration);
        when(clientContext.getLwM2mClients()).thenReturn(List.of(client));
        return client;
    }

    private void verifySharedStateIsKept() throws Exception {
        verify(clientContext, never()).unregister(any(), any());
        verify(sessionStore, never()).remove(anyString());
        verify(securityStore, never()).remove(anyString(), anyString());
        verify(registrationStore, never()).removeRegistration(anyString());
    }

    private static Registration registration(Date lastUpdate) {
        Registration registration = mock(Registration.class);
        when(registration.getId()).thenReturn(REGISTRATION_ID);
        when(registration.getEndpoint()).thenReturn(ENDPOINT);
        when(registration.getLastUpdate()).thenReturn(lastUpdate);
        return registration;
    }

}
