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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for deleting a resource
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for delete security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeleteTest {

    private final org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper();

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
    public void delete_created_object_instance() throws InterruptedException {
        // create ACL instance
        helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(3, 33))));

        // try to delete this instance
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.DELETED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_resource() throws InterruptedException {
        // create ACL instance
        helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(0, 123))));

        // try to delete this resource using coap API as lwm2m API does not allow it.
        Request delete = Request.newDelete();
        delete.getOptions().addUriPath("2").addUriPath("0").addUriPath("0");
        Response response = helper.server.coap().send(helper.getCurrentRegistration(), delete);

        // verify result
        assertEquals(org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST, response.getCode());
    }

    @Test
    public void cannot_delete_unknown_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_device_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(3, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_security_object_instance() throws InterruptedException {
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

}
