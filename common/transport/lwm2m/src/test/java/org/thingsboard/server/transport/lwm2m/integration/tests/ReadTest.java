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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for read security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.leshan.core.ResponseCode.*;
import static org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class ReadTest {

    public org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper();

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
    public void can_read_empty_object() throws InterruptedException {
        // read ACL object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(2, object.getId());
        assertTrue(object.getInstances().isEmpty());
    }

    @Test
    public void can_read_object() throws InterruptedException {
        // read device object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(3, object.getId());

        LwM2mObjectInstance instance = object.getInstance(0);
        assertEquals(0, instance.getId());
    }

    @Test
    public void can_read_object_instance() throws InterruptedException {
        // read device single instance
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mObjectInstance instance = (LwM2mObjectInstance) response.getContent();
        assertEquals(0, instance.getId());
    }

    @Test
    public void can_read_resource() throws InterruptedException {
        // read device model number
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mResource resource = (LwM2mResource) response.getContent();
        assertEquals(1, resource.getId());
        assertEquals(org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.MODEL_NUMBER, resource.getValue());
    }

    @Test
    public void can_read_empty_opaque_resource() throws InterruptedException {
        // read device model number
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.OPAQUE, TEST_OBJECT_ID, 1, OPAQUE_RESOURCE_ID));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mResource resource = (LwM2mResource) response.getContent();
        assertEquals(Type.OPAQUE, resource.getType());
        assertEquals(0, ((byte[]) resource.getValue()).length);
    }

    @Test
    public void cannot_read_non_readable_resource() throws InterruptedException {
        // read device reboot resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 4));

        // verify result
        assertEquals(METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_object() throws InterruptedException {
        // read object "50"
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_instance() throws InterruptedException {
        // read 2nd Device resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 1));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_resource() throws InterruptedException {
        // read device 50 resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_security_resource() throws InterruptedException {
        // read device 50 resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(0, 0, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
