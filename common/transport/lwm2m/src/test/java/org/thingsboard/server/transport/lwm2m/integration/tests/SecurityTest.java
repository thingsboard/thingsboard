/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.exception.EndpointMismatchException;
import org.eclipse.californium.elements.util.SimpleMessageCallback;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;

import static org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.LIFETIME;
import static org.thingsboard.server.transport.lwm2m.integration.tests.SecureIntegrationTestHelper.*;
import static org.junit.Assert.*;

public class SecurityTest {

    protected org.thingsboard.server.transport.lwm2m.integration.tests.SecureIntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.SecureIntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
    }

    @After
    public void stop() {
        if (helper.client != null)
            helper.client.destroy(true);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void registered_device_with_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();
    }

    @Test
    public void dont_sent_request_if_identity_change()
            throws NonUniqueSecurityInfoException, InterruptedException, IOException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Ensure we can send a read request
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1));

        // Add new credential to the server
        helper.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, "anotherPSK", GOOD_PSK_KEY));

        // Create new session with new credentials at client side.
        // Get connector
        Endpoint endpoint = helper.client.coap().getServer()
                .getEndpoint(helper.client.getAddress(helper.getCurrentRegisteredServer()));
        DTLSConnector connector = (DTLSConnector) ((CoapEndpoint) endpoint).getConnector();
        // Clear DTLS session to force new handshake
        connector.clearConnectionState();
        // Change PSK id
        helper.setNewPsk("anotherPSK", GOOD_PSK_KEY);
        // restart connector
        connector.start();
        // send and empty message to force a new handshake with new credentials
        SimpleMessageCallback callback = new SimpleMessageCallback();
        // create a ping message
        Request request = new Request(null, Type.CON);
        request.setToken(Token.EMPTY);
        byte[] ping = new UdpDataSerializer().getByteArray(request);
        // sent it
        connector.send(
                RawData.outbound(ping, new AddressEndpointContext(helper.server.getSecuredAddress()), callback, false));
        // Wait until new handshake DTLS is done
        EndpointContext endpointContext = callback.getEndpointContext(1000);
        assertEquals(((PreSharedKeyIdentity) endpointContext.getPeerIdentity()).getIdentity(), "anotherPSK");

        // Try to send a read request this should failed with an SendFailedException.
        try {
            helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 1000);
            fail("send must failed");
        } catch (SendFailedException e) {
            assertTrue("must be caused by an EndpointMismatchException",
                    e.getCause() instanceof EndpointMismatchException);
        } finally {
            connector.stop();
            helper.client.destroy(false);
            helper.client = null;
        }
    }

    @Test
    public void register_update_deregister_reregister_device_with_psk_to_server_with_psk()
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdateAtClientSide(LIFETIME);
        helper.assertClientRegisterered();

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistrationAtServerSide(1);
        helper.assertClientNotRegisterered();

        // check new registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_update_reregister_device_with_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdateAtClientSide(LIFETIME);
        helper.assertClientRegisterered();

        // Check stop do not de-register
        helper.client.stop(false);
        helper.ensureNoDeregistration(1);
        helper.assertClientRegisterered();

        // Check new registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        helper.assertClientRegisterered();
        Registration newRegistration = helper.getCurrentRegistration();
        assertNotEquals(registration.getId(), newRegistration.getId());

    }

    @Test
    public void server_initiates_dtls_handshake() throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        ((DTLSConnector) helper.server.coap().getSecuredEndpoint().getConnector()).clearConnectionState();

        // try to send request
        ReadResponse readResponse = helper.server.send(registration, new ReadRequest(3), 1000);
        assertTrue(readResponse.isSuccess());

        // ensure we have a new session for it
        DTLSSession session = ((DTLSConnector) helper.server.coap().getSecuredEndpoint().getConnector())
                .getSessionByAddress(registration.getSocketAddress());
        assertNotNull(session);
    }

    @Test
    public void server_initiates_dtls_handshake_timeout() throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        ((DTLSConnector) helper.server.coap().getSecuredEndpoint().getConnector()).clearConnectionState();

        // stop client
        helper.client.stop(false);

        // try to send request synchronously
        ReadResponse readResponse = helper.server.send(registration, new ReadRequest(3), 1000);
        assertNull(readResponse);

        // try to send request asynchronously
        org.thingsboard.server.transport.lwm2m.integration.tests.Callback<ReadResponse> callback = new org.thingsboard.server.transport.lwm2m.integration.tests.Callback<>();
        helper.server.send(registration, new ReadRequest(3), 1000, callback, callback);
        callback.waitForResponse(1100);
        assertTrue(callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.DTLS_HANDSHAKE_TIMEOUT,
                ((TimeoutException) callback.getException()).getType());

    }

    @Test
    public void server_does_not_initiate_dtls_handshake_with_queue_mode()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClientUsingQueueMode();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        ((DTLSConnector) helper.server.coap().getSecuredEndpoint().getConnector()).clearConnectionState();

        // try to send request
        try {
            helper.server.send(registration, new ReadRequest(3), 1000);
            fail("Read request SHOULD have failed");
        } catch (UnconnectedPeerException e) {
            // expected result
            assertFalse("client is still awake", helper.server.getPresenceService().isClientAwake(registration));
        }
    }

    @Test
    public void registered_device_with_bad_psk_identity_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials with BAD PSK ID to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), BAD_PSK_ID, GOOD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_bad_psk_key_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials with BAD PSK KEY to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, BAD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials for another endpoint to the server
        helper.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_psk_identity_to_server_with_psk_then_remove_security_info()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void nonunique_psk_identity() throws NonUniqueSecurityInfoException {
        helper.createServer();
        helper.server.start();

        EditableSecurityStore ess = helper.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
            fail("Non-unique PSK identity should throw exception on add");
        } catch (NonUniqueSecurityInfoException e) {
        }
    }

    @Test
    public void change_psk_identity_cleanup() throws NonUniqueSecurityInfoException {
        helper.createServer();
        helper.server.start();

        EditableSecurityStore ess = helper.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, BAD_PSK_ID, BAD_PSK_KEY));
        // Change PSK id for endpoint
        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        // Original/old PSK id should not be reserved any more
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, BAD_PSK_ID, BAD_PSK_KEY));
        } catch (NonUniqueSecurityInfoException e) {
            fail("PSK identity change for existing endpoint should have cleaned up old PSK identity");
        }
    }

    @Test
    public void registered_device_with_rpk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());
    }

    @Test
    public void registered_device_with_bad_rpk_to_server_with_rpk_() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        // as it is complex to create a public key, I use the server one :p as bad client public key
        PublicKey bad_client_public_key = helper.getServerPublicKey();
        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), bad_client_public_key));

        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_rpk_to_server_with_rpk_then_remove_security_info()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(BAD_ENDPOINT, helper.clientPublicKey));

        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_then_remove_security_info()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_self_signed_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverX509CertSelfSigned);
        helper.server.start();

        helper.createX509CertClient(helper.clientX509Cert, helper.clientPrivateKeyFromCert,
                helper.serverX509CertSelfSigned);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());
    }

    @Test
    public void registered_device_with_x509cert_and_bad_endpoint_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(BAD_ENDPOINT));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_and_bad_cn_certificate_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient(helper.clientX509CertWithBadCN);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(GOOD_ENDPOINT));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_and_bad_private_key_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        // we use the RPK private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = helper.clientPrivateKey;

        helper.createX509CertClient(helper.clientX509Cert, badPrivateKey);
        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_untrusted_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient(helper.clientX509CertNotTrusted);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_selfsigned_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.createX509CertClient(helper.clientX509CertSelfSigned);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_rpk()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createX509CertClient(helper.clientX509Cert);

        helper.getSecurityStore().add(
                SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientX509Cert.getPublicKey()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_rpk_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert();
        helper.server.start();

        boolean useServerCertifcatePublicKey = true;
        helper.createRPKClient(useServerCertifcatePublicKey);
        helper.client.start();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());
    }
}
