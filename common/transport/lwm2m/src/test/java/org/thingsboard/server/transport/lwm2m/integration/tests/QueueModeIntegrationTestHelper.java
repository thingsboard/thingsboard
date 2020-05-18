/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.queue.StaticClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.Registration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class QueueModeIntegrationTestHelper extends IntegrationTestHelper {

    public static final long LIFETIME = 3600; // Updates are manually triggered with a timer

    protected SynchronousPresenceListener presenceListener = new SynchronousPresenceListener() {
        @Override
        public boolean accept(Registration registration) {
            return (registration != null && registration.getEndpoint().equals(currentEndpointIdentifier.get()));
        }
    };

    protected PresenceCounter presenceCounter = new PresenceCounter() {
        @Override
        public boolean accept(Registration registration) {
            return (registration != null && registration.getEndpoint().equals(currentEndpointIdentifier.get()));
        }
    };

    @Override
    public void createClient() {
        // Create objects Enabler
        ObjectsInitializer initializer = new ObjectsInitializer(new StaticModel(createObjectModels()));
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(
                "coap://" + server.getUnsecuredAddress().getHostString() + ":" + server.getUnsecuredAddress().getPort(),
                12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.UQ, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new TestDevice("Eclipse Leshan", MODEL_NUMBER, "12345", "UQ"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setDummyInstancesForObject(2000);
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        // Build Client
        LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier.get());
        builder.setObjects(objects);
        client = builder.build();
        setupClientMonitoring();
    }

    public void createServer(int clientAwakeTime) {
        server = createServerBuilder(clientAwakeTime).build();
        server.getPresenceService().addListener(presenceCounter);
        server.getPresenceService().addListener(presenceListener);
        // monitor client registration
        setupServerMonitoring();
    }

    protected LeshanServerBuilder createServerBuilder(int clientAwakeTime) {
        LeshanServerBuilder builder = super.createServerBuilder();
        builder.setClientAwakeTimeProvider(new StaticClientAwakeTimeProvider(clientAwakeTime));
        return builder;
    }

    public void waitToSleep(long timeInMilliseconds) {
        try {
            presenceListener.waitForSleep(timeInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureAwakeFor(long awaketimeInSeconds, long margeInMs) {
        try {
            long start = System.currentTimeMillis();
            long awaketimeInMs = awaketimeInSeconds * 1000;
            presenceListener.waitForSleep(awaketimeInMs + margeInMs, TimeUnit.MILLISECONDS);
            long waitingTime = System.currentTimeMillis() - start;
            long expectedTime = awaketimeInMs - margeInMs;
            if (waitingTime < expectedTime) {
                fail(String.format(
                        "Client was not awake the expected among of time. expected : less than %dms, bu was %dms",
                        expectedTime, waitingTime));
            }
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitToGetAwake(long timeInMilliseconds) {
        try {
            presenceListener.waitForAwake(timeInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureClientAwake() {
        assertTrue(server.getPresenceService().isClientAwake(getCurrentRegistration()));
    }

    public void ensureClientSleeping() {
        assertFalse(server.getPresenceService().isClientAwake(getCurrentRegistration()));
    }

    public void ensureReceivedRequest(LwM2mResponse response) {
        assertNotNull(response);
    }

    public void ensureTimeoutException(LwM2mResponse response) {
        assertNull(response);
    }

}
