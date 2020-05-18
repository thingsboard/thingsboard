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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for create security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.leshan.core.request.ContentFormat.JSON;
import static org.eclipse.leshan.core.request.ContentFormat.TLV;
import static org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CreateTest {

    IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void can_create_instance_without_instance_id_tlv() throws InterruptedException {
        can_create_instance_without_instance_id(TLV);
    }

    @Test(expected = InvalidRequestException.class)
    public void can_create_instance_without_instance_id_json() throws InterruptedException {
        can_create_instance_without_instance_id(JSON);
    }

    public void can_create_instance_without_instance_id(ContentFormat format) throws InterruptedException {
        // create ACL instance
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(format, 2, LwM2mSingleResource.newIntegerResource(3, 33)));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // create a second ACL instance
        response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(format, 2, LwM2mSingleResource.newIntegerResource(3, 34)));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/1", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(33l, object.getInstance(0).getResource(3).getValue());
        assertEquals(34l, object.getInstance(1).getResource(3).getValue());
    }

    @Test
    public void can_create_instance_with_id_tlv() throws InterruptedException {
        can_create_instance_with_id(TLV);
    }

    @Test
    public void can_create_instance_with_id_json() throws InterruptedException {
        can_create_instance_with_id(JSON);
    }

    public void can_create_instance_with_id(ContentFormat format) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(format, 2, instance));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
    }

    @Test
    public void can_create_2_instances_of_object_tlv() throws InterruptedException {
        can_create_2_instances_of_object(TLV);
    }

    @Test
    public void can_create_2_instances_of_object_json() throws InterruptedException {
        can_create_2_instances_of_object(JSON);
    }

    public void can_create_2_instances_of_object(ContentFormat format) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        LwM2mObjectInstance instance2 = new LwM2mObjectInstance(13, LwM2mSingleResource.newIntegerResource(3, 124));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(format, 2, instance1, instance2));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
        assertEquals(object.getInstance(13).getResource(3).getValue(), 124l);
    }

    @Test
    public void cannot_create_instance_of_absent_object() throws InterruptedException {
        // try to create an instance of object 50
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(50, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_create_instance_without_all_required_resources() throws InterruptedException {
        // create ACL instance without any resources
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(TEST_OBJECT_ID, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // create ACL instance with only 1 mandatory resources (1 missing)
        CreateResponse response2 = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(TEST_OBJECT_ID,
                LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 12)));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response2.getCode());

        // create ACL instance
        LwM2mObjectInstance instance0 = new LwM2mObjectInstance(0,
                LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 22),
                LwM2mSingleResource.newStringResource(STRING_MANDATORY_RESOURCE_ID, "string"));
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(1,
                LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 22));

        CreateResponse response3 = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(TEST_OBJECT_ID, instance0, instance1));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response3.getCode());

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_create_mandatory_single_object() throws InterruptedException {
        // try to create another instance of device object
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(3, new LwM2mResource[] { LwM2mSingleResource.newStringResource(3, "v123") }));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_create_instance_of_security_object() throws InterruptedException {
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(0, new LwM2mResource[] { LwM2mSingleResource.newStringResource(0, "new.dest") }));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

}
