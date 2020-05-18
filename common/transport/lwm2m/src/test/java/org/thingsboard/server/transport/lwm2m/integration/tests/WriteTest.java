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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for write security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for update and replace instances
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.WriteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class WriteTest {
    private org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper helper = new org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper();

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
    public void can_write_string_resource_in_text() throws InterruptedException {
        write_string_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_string_resource_in_tlv() throws InterruptedException {
        write_string_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_string_resource_in__old_tlv() throws InterruptedException {
        write_string_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_string_resource_in_json() throws InterruptedException {
        write_string_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_string_resource_in__old_json() throws InterruptedException {
        write_string_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_string_resource(ContentFormat format) throws InterruptedException {
        // write resource
        String expectedvalue = "stringvalue";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, STRING_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, STRING_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_boolean_resource_in_text() throws InterruptedException {
        write_boolean_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_boolean_resource_in_tlv() throws InterruptedException {
        write_boolean_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_boolean_resource_in_old_tlv() throws InterruptedException {
        write_boolean_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_boolean_resource_in_json() throws InterruptedException {
        write_boolean_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_boolean_resource_in_old_json() throws InterruptedException {
        write_boolean_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_boolean_resource(ContentFormat format) throws InterruptedException {
        // write resource
        boolean expectedvalue = true;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_integer_resource_in_text() throws InterruptedException {
        write_integer_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_integer_resource_in_tlv() throws InterruptedException {
        write_integer_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_integer_resource_in_old_tlv() throws InterruptedException {
        write_integer_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_integer_resource_in_json() throws InterruptedException {
        write_integer_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_integer_resource_in_old_json() throws InterruptedException {
        write_integer_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_integer_resource(ContentFormat format) throws InterruptedException {
        // write resource
        long expectedvalue = 999l;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_float_resource_in_text() throws InterruptedException {
        write_float_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_float_resource_in_tlv() throws InterruptedException {
        write_float_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_float_resource_in_old_tlv() throws InterruptedException {
        write_float_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_float_resource_in_json() throws InterruptedException {
        write_float_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_float_resource_in_old_json() throws InterruptedException {
        write_float_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_float_resource(ContentFormat format) throws InterruptedException {
        // write resource
        double expectedvalue = 999.99;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_time_resource_in_text() throws InterruptedException {
        write_time_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_time_resource_in_tlv() throws InterruptedException {
        write_time_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_time_resource_in_old_tlv() throws InterruptedException {
        write_time_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_time_resource_in_json() throws InterruptedException {
        write_time_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_time_resource_in_old_json() throws InterruptedException {
        write_time_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_time_resource(ContentFormat format) throws InterruptedException {
        // write resource
        Date expectedvalue = new Date(946681000l); // second accuracy
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, TIME_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, TIME_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_opaque_resource_in_opaque() throws InterruptedException {
        write_opaque_resource(ContentFormat.OPAQUE);
    }

    @Test
    public void can_write_opaque_resource_in_tlv() throws InterruptedException {
        write_opaque_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_opaque_resource_in_old_tlv() throws InterruptedException {
        write_opaque_resource(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_opaque_resource_in_json() throws InterruptedException {
        write_opaque_resource(ContentFormat.JSON);
    }

    @Test
    public void can_write_opaque_resource_in_old_json() throws InterruptedException {
        write_opaque_resource(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    private void write_opaque_resource(ContentFormat format) throws InterruptedException {
        // write resource
        byte[] expectedvalue = new byte[] { 1, 2, 3 };
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertArrayEquals(expectedvalue, (byte[]) resource.getValue());
    }

    @Test
    public void cannot_write_non_writable_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String manufacturer = "new manufacturer";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 0, manufacturer));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_write_security_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String uri = "new.dest.server";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(0, 0, 0, uri));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_write_object_instance_in_tlv() throws InterruptedException {
        can_write_object_instance(ContentFormat.TLV);
    }

    @Test
    public void can_write_object_instance_in_old_tlv() throws InterruptedException {
        can_write_object_instance(ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE));
    }

    @Test
    public void can_write_object_instance_in_json() throws InterruptedException {
        can_write_object_instance(ContentFormat.JSON);
    }

    @Test
    public void can_write_object_instance_in_old_json() throws InterruptedException {
        can_write_object_instance(ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE));
    }

    public void can_write_object_instance(ContentFormat format) throws InterruptedException {
        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.REPLACE, format, 3, 0, utcOffset, timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }

    @Test
    public void can_write_replacing_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        LwM2mResource notificationStoring = LwM2mSingleResource.newBooleanResource(6, false);
        LwM2mResource binding = LwM2mSingleResource.newStringResource(7, "U");
        response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod, notificationStoring, binding));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        assertEquals(notificationStoring, instance.getResource(6));
        assertEquals(binding, instance.getResource(7));
        assertNull(instance.getResource(3)); // removed not contained optional writable resource
    }

    @Test
    public void cannot_write_replacing_incomplete_object_instance() throws InterruptedException {
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_write_updating_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.UPDATE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        // no resources are removed when updating
        assertNotNull(instance.getResource(3));
        assertNotNull(instance.getResource(6));
        assertNotNull(instance.getResource(7));
    }

    @Test
    public void can_write_multi_instance_objlnk_resource_in_tlv() throws InterruptedException {
        Map<Integer, ObjectLink> neighbourCellReport = new HashMap<>();
        neighbourCellReport.put(0, new ObjectLink(10245, 1));
        neighbourCellReport.put(1, new ObjectLink(10242, 2));
        neighbourCellReport.put(2, new ObjectLink(10244, 3));

        // Write objlnk resource in TLV format
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(ContentFormat.TLV, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0,
                        org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_MULTI_INSTANCE_RESOURCE_ID, neighbourCellReport, Type.OBJLNK));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(
                org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_MULTI_INSTANCE_RESOURCE_ID));
        LwM2mMultipleResource resource = (LwM2mMultipleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue(0)).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue(0)).getObjectInstanceId(), 1);
        assertEquals(((ObjectLink) resource.getValue(1)).getObjectId(), 10242);
        assertEquals(((ObjectLink) resource.getValue(1)).getObjectInstanceId(), 2);
        assertEquals(((ObjectLink) resource.getValue(2)).getObjectId(), 10244);
        assertEquals(((ObjectLink) resource.getValue(2)).getObjectInstanceId(), 3);
    }

    @Test
    public void can_write_single_instance_objlnk_resource_in_tlv() throws InterruptedException {
        ObjectLink data = new ObjectLink(10245, 1);

        // Write objlnk resource in TLV format
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(ContentFormat.TLV, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0,
                        org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, data));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(
                org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID));
        LwM2mSingleResource resource = (LwM2mSingleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue()).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue()).getObjectInstanceId(), 1);
    }

    @Test
    public void can_write_single_instance_objlnk_resource_in_text() throws InterruptedException {
        // Write objlnk resource in TEXT format
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(ContentFormat.TEXT, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0,
                        org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, new ObjectLink(10245, 0)));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.TEXT, org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID, 0,
                        org.thingsboard.server.transport.lwm2m.integration.tests.IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID));
        LwM2mSingleResource resource = (LwM2mSingleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue()).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue()).getObjectInstanceId(), 0);
    }

    @Test(expected = CodecException.class)
    public void send_writerequest_synchronously_with_bad_payload_raises_codeexception() throws InterruptedException {
        helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 13, "a string instead of timestamp for currenttime resource"));

    }

    @Test(expected = CodecException.class)
    public void send_writerequest_asynchronously_with_bad_payload_raises_codeexception() throws InterruptedException {
        helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 13, "a string instead of timestamp for currenttime resource"),
                new ResponseCallback<WriteResponse>() {
                    @Override
                    public void onResponse(WriteResponse response) {
                    }
                }, new ErrorCallback() {
                    @Override
                    public void onError(Exception e) {
                    }
                });
    }
}
