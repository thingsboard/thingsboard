/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class FailingTest {

    public org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper() {
        @Override
        protected LeshanServerBuilder createServerBuilder() {
            NetworkConfig coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();

            // configure retransmission, with this configuration a request without ACK should timeout in ~200*5ms
            coapConfig.setInt(NetworkConfig.Keys.ACK_TIMEOUT, 200).setFloat(NetworkConfig.Keys.ACK_RANDOM_FACTOR, 1f)
                    .setFloat(NetworkConfig.Keys.ACK_TIMEOUT_SCALE, 1f).setInt(NetworkConfig.Keys.MAX_RETRANSMIT, 4);

            LeshanServerBuilder builder = super.createServerBuilder();
            builder.setCoapConfig(coapConfig);

            return builder;
        };
    };

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
    }

    @After
    public void stop() {
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void sync_send_without_acknowleged() throws Exception {
        // Register client
        org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient client = new org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient(helper.server.getUnsecuredAddress());
        client.sendLwM2mRequest(new RegisterRequest(helper.getCurrentEndpoint(), 60l, null, BindingMode.U, null,
                Link.parse("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().go();
        helper.waitForRegistrationAtServerSide(1);

        // Send read
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000);
            }
        });
        // Request should timedout in ~1s we don't send ACK
        ReadResponse response = future.get(1500, TimeUnit.MILLISECONDS);
        Assert.assertNull("we should timeout", response);
    }

    @Test
    public void sync_send_with_acknowleged_request_without_response() throws Exception {
        // Register client
        org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient client = new org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient(helper.server.getUnsecuredAddress());
        client.sendLwM2mRequest(new RegisterRequest(helper.getCurrentEndpoint(), 60l, null, BindingMode.U, null,
                Link.parse("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().go();
        helper.waitForRegistrationAtServerSide(1);

        // Send read
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000);
            }
        });

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send the ACK
        Thread.sleep(1500);
        Assert.assertFalse("we should still wait for response", future.isDone());
        ReadResponse response = future.get(2000, TimeUnit.MILLISECONDS);
        Assert.assertNull("we should timeout", response);
    }

    @Test
    public void async_send_without_acknowleged() throws Exception {
        // register client
        org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient client = new org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient(helper.server.getUnsecuredAddress());
        client.sendLwM2mRequest(new RegisterRequest(helper.getCurrentEndpoint(), 60l, null, BindingMode.U, null,
                Link.parse("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().go();
        helper.waitForRegistrationAtServerSide(1);

        // send read
        Callback<ReadResponse> callback = new Callback<ReadResponse>();
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000l, callback, callback);

        // Request should timedout in ~1s we don't send ACK
        callback.waitForResponse(1500);
        Assert.assertTrue("we should timeout", callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.COAP_TIMEOUT, ((TimeoutException) callback.getException()).getType());
    }

    @Test
    public void async_send_with_acknowleged_request_without_response() throws Exception {
        // register client
        org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient client = new org.thingsboard.server.transport.lwm2m.integration.tests.LockStepLwM2mClient(helper.server.getUnsecuredAddress());
        client.sendLwM2mRequest(new RegisterRequest(helper.getCurrentEndpoint(), 60l, null, BindingMode.U, null,
                Link.parse("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().go();
        helper.waitForRegistrationAtServerSide(1);

        // send read
        Callback<ReadResponse> callback = new Callback<ReadResponse>();
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000l, callback, callback);

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send a ack
        Thread.sleep(1500);
        Assert.assertTrue("we should still wait for response", callback.getException() == null);
        callback.waitForResponse(2000);
        Assert.assertTrue("we should timeout", callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.RESPONSE_TIMEOUT, ((TimeoutException) callback.getException()).getType());
    }
}
