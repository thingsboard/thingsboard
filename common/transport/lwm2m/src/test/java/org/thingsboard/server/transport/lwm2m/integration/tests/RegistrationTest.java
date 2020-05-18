/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.LIFETIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class RegistrationTest {

    protected org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
    }

    @After
    public void stop() throws InterruptedException {
        helper.client.destroy(true);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void register_update_deregister() {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>,</2000/1>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());

        // Check for update
        helper.waitForUpdateAtClientSide(LIFETIME);
        helper.assertClientRegisterered();

        // Check deregistration
        helper.client.stop(true);
        helper.waitForDeregistrationAtServerSide(1);
        helper.assertClientNotRegisterered();
    }

    @Test
    public void deregister_cancel_multiple_pending_request() throws InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>,</2000/1>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());

        // Stop client with out de-registration
        helper.waitForRegistrationAtClientSide(1);
        helper.client.stop(false);

        // Send multiple reads which should be retransmitted.
        List<org.thingsboard.server.transport.lwm2m.integration.tests.Callback<ReadResponse>> callbacks = new ArrayList<>();

        for (int index = 0; index < 4; ++index) {
            org.thingsboard.server.transport.lwm2m.integration.tests.Callback<ReadResponse> callback = new org.thingsboard.server.transport.lwm2m.integration.tests.Callback<>();
            helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), callback, callback);
            callbacks.add(callback);
        }

        // Restart client (de-registration/re-registration)
        helper.client.start();

        // Check the request was cancelled.
        int index = 0;
        for (org.thingsboard.server.transport.lwm2m.integration.tests.Callback<ReadResponse> callback : callbacks) {
            boolean timedout = !callback.waitForResponse(1000);
            assertFalse("Response or Error expected, no timeout, call " + index, timedout);
            assertTrue("Response or Error expected, call " + index, callback.isCalled().get());
            assertNull("No response expected, call " + index, callback.getResponse());
            assertNotNull("Exception expected, call " + index, callback.getException());
            ++index;
        }
    }

    @Test
    public void register_update_deregister_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
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

        // Check new registration
        helper.waitForDeregistrationAtClientSide(1);
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_update_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
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

        // check stop do not de-register
        helper.client.stop(false);
        helper.ensureNoDeregistration(1);
        helper.assertClientRegisterered();

        // check new registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_observe_deregister_observe() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // check observation registry is not null
        Registration currentRegistration = helper.getCurrentRegistration();
        Set<Observation> observations = helper.server.getObservationService().getObservations(currentRegistration);
        assertEquals(1, observations.size());
        Observation obs = observations.iterator().next();
        assertEquals(currentRegistration.getId(), obs.getRegistrationId());
        assertEquals(new LwM2mPath(3, 0), obs.getPath());

        // Check de-registration
        helper.waitForRegistrationAtClientSide(1);
        helper.client.stop(true);
        helper.waitForDeregistrationAtServerSide(1);
        helper.assertClientNotRegisterered();
        helper.waitForDeregistrationAtClientSide(1);
        observations = helper.server.getObservationService().getObservations(currentRegistration);
        assertTrue(observations.isEmpty());

        // try to send a new observation
        try {
            observeResponse = helper.server.send(currentRegistration, new ObserveRequest(3, 0), 50);
        } catch (SendFailedException e) {
            return;
        }
        fail("Observe request should be sent");
    }

    @Test
    public void register_with_additional_attributes() throws InterruptedException {
        // Create client with additional attributes
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("imei", "2136872368");
        helper.createClient(additionalAttributes);

        // Check registration
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check we are registered with the expected attributes
        helper.assertClientRegisterered();
        assertNotNull(helper.getLastRegistration());
        assertEquals(additionalAttributes, helper.getLastRegistration().getAdditionalRegistrationAttributes());
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>,</2000/1>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());
    }

    @Test
    public void register_with_invalid_request() throws InterruptedException, IOException {
        // Check registration
        helper.assertClientNotRegisterered();

        // create a register request without the list of supported object
        Request coapRequest = new Request(Code.POST);
        coapRequest.setDestinationContext(new AddressEndpointContext(helper.server.getUnsecuredAddress()));
        coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        coapRequest.getOptions().addUriPath("rd");
        coapRequest.getOptions().addUriQuery("ep=" + helper.currentEndpointIdentifier);

        // send request
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setInetSocketAddress(new InetSocketAddress(0));
        CoapEndpoint coapEndpoint = builder.build();
        coapEndpoint.start();
        coapEndpoint.sendRequest(coapRequest);

        // check response
        Response response = coapRequest.waitForResponse(1000);
        assertEquals(response.getCode(), org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST);
        coapEndpoint.stop();
    }
}
