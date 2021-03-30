/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseDeviceProfileControllerTest extends AbstractControllerTest {

    private IdComparator<DeviceProfile> idComparator = new IdComparator<>();
    private IdComparator<DeviceProfileInfo> deviceProfileInfoIdComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        Assert.assertNotNull(savedDeviceProfile.getId());
        Assert.assertTrue(savedDeviceProfile.getCreatedTime() > 0);
        Assert.assertEquals(deviceProfile.getName(), savedDeviceProfile.getName());
        Assert.assertEquals(deviceProfile.getDescription(), savedDeviceProfile.getDescription());
        Assert.assertEquals(deviceProfile.getProfileData(), savedDeviceProfile.getProfileData());
        Assert.assertEquals(deviceProfile.isDefault(), savedDeviceProfile.isDefault());
        Assert.assertEquals(deviceProfile.getDefaultRuleChainId(), savedDeviceProfile.getDefaultRuleChainId());
        savedDeviceProfile.setName("New device profile");
        doPost("/api/deviceProfile", savedDeviceProfile, DeviceProfile.class);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString(), DeviceProfile.class);
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testFindDeviceProfileById() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString(), DeviceProfile.class);
        Assert.assertNotNull(foundDeviceProfile);
        Assert.assertEquals(savedDeviceProfile, foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfileInfoById() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfileInfo foundDeviceProfileInfo = doGet("/api/deviceProfileInfo/"+savedDeviceProfile.getId().getId().toString(), DeviceProfileInfo.class);
        Assert.assertNotNull(foundDeviceProfileInfo);
        Assert.assertEquals(savedDeviceProfile.getId(), foundDeviceProfileInfo.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfileInfo.getName());
        Assert.assertEquals(savedDeviceProfile.getType(), foundDeviceProfileInfo.getType());
    }

    @Test
    public void testFindDefaultDeviceProfileInfo() throws Exception {
        DeviceProfileInfo foundDefaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getId());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getName());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getType());
        Assert.assertEquals(DeviceProfileType.DEFAULT, foundDefaultDeviceProfileInfo.getType());
        Assert.assertEquals("default", foundDefaultDeviceProfileInfo.getName());
    }

    @Test
    public void testSetDefaultDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile 1", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfile defaultDeviceProfile = doPost("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString()+"/default", null, DeviceProfile.class);
        Assert.assertNotNull(defaultDeviceProfile);
        DeviceProfileInfo foundDefaultDeviceProfile = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
        Assert.assertNotNull(foundDefaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile.getName(), foundDefaultDeviceProfile.getName());
        Assert.assertEquals(savedDeviceProfile.getId(), foundDefaultDeviceProfile.getId());
        Assert.assertEquals(savedDeviceProfile.getType(), foundDefaultDeviceProfile.getType());
    }

    @Test
    public void testSaveDeviceProfileWithEmptyName() throws Exception {
        DeviceProfile deviceProfile = new DeviceProfile();
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device profile name should be specified")));
    }

    @Test
    public void testSaveDeviceProfileWithSameName() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isOk());
        DeviceProfile deviceProfile2 = this.createDeviceProfile("Device Profile", null);
        doPost("/api/deviceProfile", deviceProfile2).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device profile with such name already exists")));
    }

    @Test
    public void testSaveDeviceProfileWithSameProvisionDeviceKey() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        deviceProfile.setProvisionDeviceKey("testProvisionDeviceKey");
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isOk());
        DeviceProfile deviceProfile2 = this.createDeviceProfile("Device Profile 2", null);
        deviceProfile2.setProvisionDeviceKey("testProvisionDeviceKey");
        doPost("/api/deviceProfile", deviceProfile2).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device profile with such provision device key already exists")));
    }

    @Ignore
    @Test
    public void testChangeDeviceProfileTypeWithExistingDevices() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        doPost("/api/device", device, Device.class);
        //TODO uncomment once we have other device types;
        //savedDeviceProfile.setType(DeviceProfileType.LWM2M);
        doPost("/api/deviceProfile", savedDeviceProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't change device profile type because devices referenced it")));
    }

    @Test
    public void testChangeDeviceProfileTransportTypeWithExistingDevices() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        doPost("/api/device", device, Device.class);
        savedDeviceProfile.setTransportType(DeviceTransportType.MQTT);
        doPost("/api/deviceProfile", savedDeviceProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't change device profile transport type because devices referenced it")));
    }

    @Test
    public void testDeleteDeviceProfileWithExistingDevice() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());

        Device savedDevice = doPost("/api/device", device, Device.class);

        doDelete("/api/deviceProfile/" + savedDeviceProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The device profile referenced by the devices cannot be deleted")));
    }

    @Test
    public void testDeleteDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        doDelete("/api/deviceProfile/" + savedDeviceProfile.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/deviceProfile/" + savedDeviceProfile.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindDeviceProfiles() throws Exception {
        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        deviceProfiles.addAll(pageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile"+i, null);
            deviceProfiles.add(doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class));
        }

        List<DeviceProfile> loadedDeviceProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                    new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
            loadedDeviceProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfiles, idComparator);

        Assert.assertEquals(deviceProfiles, loadedDeviceProfiles);

        for (DeviceProfile deviceProfile : loadedDeviceProfiles) {
            if (!deviceProfile.isDefault()) {
                doDelete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindDeviceProfileInfos() throws Exception {
        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> deviceProfilePageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(deviceProfilePageData.hasNext());
        Assert.assertEquals(1, deviceProfilePageData.getTotalElements());
        deviceProfiles.addAll(deviceProfilePageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile"+i, null);
            deviceProfiles.add(doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class));
        }

        List<DeviceProfileInfo> loadedDeviceProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<DeviceProfileInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/deviceProfileInfos?",
                    new TypeReference<PageData<DeviceProfileInfo>>(){}, pageLink);
            loadedDeviceProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfileInfos, deviceProfileInfoIdComparator);

        List<DeviceProfileInfo> deviceProfileInfos = deviceProfiles.stream().map(deviceProfile -> new DeviceProfileInfo(deviceProfile.getId(),
                deviceProfile.getName(), deviceProfile.getType(), deviceProfile.getTransportType())).collect(Collectors.toList());

        Assert.assertEquals(deviceProfileInfos, loadedDeviceProfileInfos);

        for (DeviceProfile deviceProfile : deviceProfiles) {
            if (!deviceProfile.isDefault()) {
                doDelete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/deviceProfileInfos?",
                new TypeReference<PageData<DeviceProfileInfo>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testSaveProtoDeviceProfileWithInvalidProtoFile() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SchemaValidationTest {\n" +
                "   required int32 parameter = 1;\n" +
                "}", "[Transport Configuration] failed to parse attributes proto schema due to: Syntax error in :6:4: 'required' label forbidden in proto3 field declarations");
    }

    @Test
    public void testSaveProtoDeviceProfileWithInvalidProtoSyntax() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto2\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SchemaValidationTest {\n" +
                "   required int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid schema syntax: proto2 for attributes proto schema provided! Only proto3 allowed!");
    }

    @Test
    public void testSaveProtoDeviceProfileOptionsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "option java_package = \"com.test.schemavalidation\";\n" +
                "option java_multiple_files = true;\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SchemaValidationTest {\n" +
                "   int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Schema options don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfilePublicImportsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "import public \"oldschema.proto\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SchemaValidationTest {\n" +
                "   int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Schema public imports don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileImportsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "import \"oldschema.proto\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SchemaValidationTest {\n" +
                "   int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Schema imports don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileExtendDeclarationsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "extend google.protobuf.MethodOptions {\n" +
                "  MyMessage my_method_option = 50007;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Schema extend declarations don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileEnumOptionsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "enum testEnum {\n" +
                "   option allow_alias = true;\n" +
                "   DEFAULT = 0;\n" +
                "   STARTED = 1;\n" +
                "   RUNNING = 2;\n" +
                "}\n" +
                "\n" +
                "message testMessage {\n" +
                "   int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Enum definitions options are not supported!");
    }

    @Test
    public void testSaveProtoDeviceProfileNoOneMessageTypeExists() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "enum testEnum {\n" +
                "   DEFAULT = 0;\n" +
                "   STARTED = 1;\n" +
                "   RUNNING = 2;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! At least one Message definition should exists!");
    }

    @Test
    public void testSaveProtoDeviceProfileMessageTypeOptionsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message testMessage {\n" +
                "   option allow_alias = true;\n" +
                "   int32 parameter = 1;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Message definition options don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileMessageTypeExtensionsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message TestMessage {\n" +
                "   extensions 100 to 199;\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Message definition extensions don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileMessageTypeReservedElementsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message Foo {\n" +
                "  reserved 2, 15, 9 to 11;\n" +
                "  reserved \"foo\", \"bar\";\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Message definition reserved elements don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileMessageTypeGroupsElementsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message TestMessage {\n" +
                "  repeated group Result = 1 {\n" +
                "    string url = 2;\n" +
                "    string title = 3;\n" +
                "    repeated string snippets = 4;\n" +
                "  }\n" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! Message definition groups don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileOneOfsGroupsElementsNotSupported() throws Exception {
        testSaveDeviceProfileWithInvalidProtoSchema("syntax = \"proto3\";\n" +
                "\n" +
                "package schemavalidation;\n" +
                "\n" +
                "message SampleMessage {\n" +
                "  oneof test_oneof {\n" +
                "     string name = 1;\n" +
                "     group Result = 2 {\n" +
                "    \tstring url = 3;\n" +
                "    \tstring title = 4;\n" +
                "    \trepeated string snippets = 5;\n" +
                "     }\n" +
                "  }" +
                "}", "[Transport Configuration] invalid attributes proto schema provided! OneOf definition groups don't support!");
    }

    @Test
    public void testSaveProtoDeviceProfileWithMessageNestedTypes() throws Exception {
        String schema = "syntax = \"proto3\";\n" +
                "\n" +
                "package testnested;\n" +
                "\n" +
                "message Outer {\n" +
                "  message MiddleAA {\n" +
                "    message Inner {\n" +
                "      int64 ival = 1;\n" +
                "      bool  booly = 2;\n" +
                "    }\n" +
                "    Inner inner = 1;\n" +
                "  }\n" +
                "  message MiddleBB {\n" +
                "    message Inner {\n" +
                "      int32 ival = 1;\n" +
                "      bool  booly = 2;\n" +
                "    }\n" +
                "    Inner inner = 1;\n" +
                "  }\n" +
                "  MiddleAA middleAA = 1;\n" +
                "  MiddleBB middleBB = 2;\n" +
                "}";
        DynamicSchema dynamicSchema = getDynamicSchema(schema);
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(5, messageTypes.size());
        assertTrue(messageTypes.contains("testnested.Outer"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA.Inner"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB.Inner"));

        DynamicMessage.Builder middleAAInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleAAInnerMsgDescriptor = middleAAInnerMsgBuilder.getDescriptorForType();
        DynamicMessage middleAAInnerMsg = middleAAInnerMsgBuilder
                .setField(middleAAInnerMsgDescriptor.findFieldByName("ival"), 1L)
                .setField(middleAAInnerMsgDescriptor.findFieldByName("booly"), true)
                .build();

        DynamicMessage.Builder middleAAMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA");
        Descriptors.Descriptor middleAAMsgDescriptor = middleAAMsgBuilder.getDescriptorForType();
        DynamicMessage middleAAMsg = middleAAMsgBuilder
                .setField(middleAAMsgDescriptor.findFieldByName("inner"), middleAAInnerMsg)
                .build();

        DynamicMessage.Builder middleBBInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleBBInnerMsgDescriptor = middleBBInnerMsgBuilder.getDescriptorForType();
        DynamicMessage middleBBInnerMsg = middleBBInnerMsgBuilder
                .setField(middleBBInnerMsgDescriptor.findFieldByName("ival"), 0L)
                .setField(middleBBInnerMsgDescriptor.findFieldByName("booly"), false)
                .build();

        DynamicMessage.Builder middleBBMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleBB");
        Descriptors.Descriptor middleBBMsgDescriptor = middleBBMsgBuilder.getDescriptorForType();
        DynamicMessage middleBBMsg = middleBBMsgBuilder
                .setField(middleBBMsgDescriptor.findFieldByName("inner"), middleBBInnerMsg)
                .build();


        DynamicMessage.Builder outerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer");
        Descriptors.Descriptor outerMsgBuilderDescriptor = outerMsgBuilder.getDescriptorForType();
        DynamicMessage outerMsg = outerMsgBuilder
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleAA"), middleAAMsg)
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleBB"), middleBBMsg)
                .build();

        assertEquals("{\n" +
                "  \"middleAA\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": \"1\",\n" +
                "      \"booly\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"middleBB\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": 0,\n" +
                "      \"booly\": false\n" +
                "    }\n" +
                "  }\n" +
                "}", dynamicMsgToJson(outerMsgBuilderDescriptor, outerMsg.toByteArray()));
    }

    @Test
    public void testSaveProtoDeviceProfileWithMessageOneOfs() throws Exception {
        String schema = "syntax = \"proto3\";\n" +
                "\n" +
                "package testoneofs;\n" +
                "\n" +
                "message SubMessage {\n" +
                "   repeated string name = 1;\n" +
                "}\n" +
                "\n" +
                "message SampleMessage {\n" +
                "  oneof testOneOf {\n" +
                "     string name = 4;\n" +
                "     SubMessage subMessage = 9;\n" +
                "  }\n" +
                "}";
        DynamicSchema dynamicSchema = getDynamicSchema(schema);
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(2, messageTypes.size());
        assertTrue(messageTypes.contains("testoneofs.SubMessage"));
        assertTrue(messageTypes.contains("testoneofs.SampleMessage"));

        DynamicMessage.Builder sampleMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SampleMessage");
        Descriptors.Descriptor sampleMsgDescriptor = sampleMsgBuilder.getDescriptorForType();
        assertNotNull(sampleMsgDescriptor);

        List<Descriptors.FieldDescriptor> fields = sampleMsgDescriptor.getFields();
        assertEquals(2, fields.size());
        DynamicMessage sampleMsg = sampleMsgBuilder
                .setField(sampleMsgDescriptor.findFieldByName("name"), "Bob")
                .build();
        assertEquals("{\n" + "  \"name\": \"Bob\"\n" + "}", dynamicMsgToJson(sampleMsgDescriptor, sampleMsg.toByteArray()));

        DynamicMessage.Builder subMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SubMessage");
        Descriptors.Descriptor subMsgDescriptor = subMsgBuilder.getDescriptorForType();
        DynamicMessage subMsg = subMsgBuilder
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "Alice")
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "John")
                .build();

        DynamicMessage sampleMsgWithOneOfSubMessage = sampleMsgBuilder.setField(sampleMsgDescriptor.findFieldByName("subMessage"), subMsg).build();
        assertEquals("{\n" + "  \"subMessage\": {\n" + "    \"name\": [\"Alice\", \"John\"]\n" + "  }\n" + "}",
                dynamicMsgToJson(sampleMsgDescriptor, sampleMsgWithOneOfSubMessage.toByteArray()));
    }

    private DeviceProfile testSaveDeviceProfileWithProtoPayloadType(String schema) throws Exception {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = this.createProtoTransportPayloadConfiguration(schema, schema);
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = this.createMqttDeviceProfileTransportConfiguration(protoTransportPayloadConfiguration);
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", mqttDeviceProfileTransportConfiguration);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString(), DeviceProfile.class);
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
        return savedDeviceProfile;
    }

    private void testSaveDeviceProfileWithInvalidProtoSchema(String schema, String errorMsg) throws Exception {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = this.createProtoTransportPayloadConfiguration(schema, schema);
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = this.createMqttDeviceProfileTransportConfiguration(protoTransportPayloadConfiguration);
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", mqttDeviceProfileTransportConfiguration);
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(errorMsg)));
    }

    private DynamicSchema getDynamicSchema(String schema) throws Exception {
        DeviceProfile deviceProfile = testSaveDeviceProfileWithProtoPayloadType(schema);
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttDeviceProfileTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement protoFile = protoTransportPayloadConfiguration.getTransportProtoSchema(schema);
        return protoTransportPayloadConfiguration.getDynamicSchema(protoFile, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);
    }

    private String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

}
