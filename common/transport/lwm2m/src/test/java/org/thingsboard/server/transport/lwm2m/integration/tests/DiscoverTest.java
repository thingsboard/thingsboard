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
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DiscoverTest {

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_discover_object() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        Link[] payload = response.getObjectLinks();
        assertEquals("</3>,</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/11>,</3/0/14>,</3/0/15>,</3/0/16>",
                Link.serialize(payload));
    }

    @Test
    public void cant_discover_non_existent_object() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(4));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_discover_object_instance() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        Link[] payload = response.getObjectLinks();
        assertEquals("</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/11>,</3/0/14>,</3/0/15>,</3/0/16>",
                Link.serialize(payload));
    }

    @Test
    public void cant_discover_non_existent_instance() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 1));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_discover_resource() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 0, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        Link[] payload = response.getObjectLinks();
        assertEquals("</3/0/0>", Link.serialize(payload));
    }

    @Test
    public void cant_discover_resource_of_non_existent_object() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(4, 0, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cant_discover_resource_of_non_existent_instance() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 1, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cant_discover_resource_of_non_existent_instance_and_resource() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 1, 20));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cant_discover_resource_of_non_existent_resource() throws InterruptedException {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getCurrentRegistration(), new DiscoverRequest(3, 0, 42));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
