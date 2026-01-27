/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EntityRelationControllerTest extends AbstractControllerTest {

    public static final String BASE_DEVICE_NAME = "Test dummy device";

    @Autowired
    private RelationDao relationDao;

    private Device mainDevice;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();

        var device = new Device();
        device.setName("Main test device");
        device.setType("default");
        mainDevice = doPost("/api/device", device, Device.class);
    }

    @Test
    public void testSaveAndFindRelation() throws Exception {
        Device device = createDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        relation = doPost("/api/v2/relation", relation, EntityRelation.class);

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        testNotifyEntityAllOneTimeRelation(foundRelation,
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, foundRelation);
    }

    @Test
    public void testSaveRelationFromValidation() throws Exception {
        // GIVEN
        var relation = new EntityRelation();
        relation.setFrom(null);
        relation.setTo(mainDevice.getId());
        relation.setType("Contains");

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            doPost(endpoint, relation)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: from must not be null")));
        }
    }

    @Test
    public void testSaveRelationToValidation() throws Exception {
        // GIVEN
        var relation = new EntityRelation();
        relation.setFrom(mainDevice.getId());
        relation.setTo(null);
        relation.setType("Contains");

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            doPost(endpoint, relation)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: to must not be null")));
        }
    }

    @Test
    public void testSaveRelationRelationTypeValidation() throws Exception {
        // GIVEN
        var device = createDevice("Test device");

        EntityRelation relationTypeNull = createFromRelation(mainDevice, device, null);
        EntityRelation relationTypeEmpty = createFromRelation(mainDevice, device, "");
        EntityRelation relationTypeBlank = createFromRelation(mainDevice, device, "  ");
        EntityRelation relationTypeContainsNullChar = createFromRelation(mainDevice, device, "null char \u0000");
        EntityRelation relationTypeTooLong = createFromRelation(mainDevice, device, "a".repeat(256));

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            doPost(endpoint, relationTypeNull)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: type must not be blank")));

            doPost(endpoint, relationTypeEmpty)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: type must not be blank")));

            doPost(endpoint, relationTypeBlank)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: type must not be blank")));

            doPost(endpoint, relationTypeContainsNullChar)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: type should not contain 0x00 symbol")));

            doPost(endpoint, relationTypeTooLong)
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(is("Validation error: type length must be equal or less than 255")));
        }
    }

    @Test
    public void testSaveRelationFromNonexistentEntity() throws Exception {
        // GIVEN
        var nonexistentDevice = new Device();
        nonexistentDevice.setId(new DeviceId(UUID.randomUUID()));
        nonexistentDevice.setName("Nonexistent device");
        nonexistentDevice.setType("default");
        EntityRelation relation = createFromRelation(nonexistentDevice, mainDevice, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            doPost(endpoint, relation)
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(is(msgErrorNoFound("Device", nonexistentDevice.getId().toString()))));

            testNotifyEntityNever(mainDevice.getId(), null);
        }
    }

    @Test
    public void testSaveRelationToNonexistentEntity() throws Exception {
        // GIVEN
        var nonexistentDevice = new Device();
        nonexistentDevice.setId(new DeviceId(UUID.randomUUID()));
        nonexistentDevice.setName("Nonexistent device");
        nonexistentDevice.setType("default");
        EntityRelation relation = createFromRelation(mainDevice, nonexistentDevice, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            doPost(endpoint, relation)
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(is(msgErrorNoFound("Device", nonexistentDevice.getId().toString()))));

            testNotifyEntityNever(mainDevice.getId(), null);
        }
    }

    @Test
    public void testSaveAndFindRelationsByFrom() throws Exception {
        final int numOfDevices = 30;

        Mockito.reset(tbClusterService, auditLogService);

        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelation relationTest = createFromRelation(mainDevice, mainDevice, "TEST_NOTIFY_ENTITY");
        testNotifyEntityAllManyRelation(relationTest, tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, numOfDevices);

        String url = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    public void testSaveAndFindRelationsByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    public void testSaveAndFindRelationsByFromWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = createDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }

    @Test
    public void testSaveAndFindRelationsByFromWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = createDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    public void testSaveAndFindRelationsByToWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = createDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }

    @Test
    public void testSaveAndFindRelationsByToWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = createDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    public void testFindRelationsInfoByFrom() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos = JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {});

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByFrom(relationsInfos);
    }

    @Test
    public void testFindRelationsInfoByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos = JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {});

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!", numOfDevices, relationsInfos.size());

        assertRelationsInfosByTo(relationsInfos);
    }

    @Test
    public void testDeleteRelation() throws Exception {
        // GIVEN
        Device device = createDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        relation = doPost("/api/v2/relation", relation, EntityRelation.class);

        Mockito.reset(tbClusterService, auditLogService);

        // WHEN
        String deleteUrl = String.format("/api/v2/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );
        var deletedRelation = doDelete(deleteUrl, EntityRelation.class);

        // THEN
        testNotifyEntityAllOneTimeRelation(deletedRelation,
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.RELATION_DELETED, deletedRelation);

        getRelation(relation)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is(msgErrorNotFound)));
    }

    @Test
    public void testDeleteRelationWithTypeEmptyOrBlank() throws Exception {
        // GIVEN
        Device device = createDevice("Test device");

        // WHEN-THEN
        for (String endpoint : List.of("/api/relation", "/api/v2/relation")) {
            // saving relation with empty type
            EntityRelation emptyRelation = createFromRelation(mainDevice, device, "");
            relationDao.saveRelation(tenantId, emptyRelation);

            // saving relation with blank type
            EntityRelation blankRelation = createFromRelation(mainDevice, device, " ");
            relationDao.saveRelation(tenantId, blankRelation);

            // deleting relation with empty type
            String deleteEmptyUrl = String.format(endpoint + "?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                    mainDevice.getUuidId(), EntityType.DEVICE,
                    emptyRelation.getType(), device.getUuidId(), EntityType.DEVICE
            );
            doDelete(deleteEmptyUrl).andExpect(status().isOk());

            getRelation(emptyRelation)
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(is(msgErrorNotFound)));

            // deleting relation with blank type
            String deleteBlankUrl = String.format(endpoint + "?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                    mainDevice.getUuidId(), EntityType.DEVICE,
                    blankRelation.getType(), device.getUuidId(), EntityType.DEVICE
            );
            doDelete(deleteBlankUrl).andExpect(status().isOk());

            getRelation(blankRelation)
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(is(msgErrorNotFound)));
        }
    }

    @Test
    public void testDeleteRelationWithOtherFromDeviceError() throws Exception {
        Device device = createDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = createDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                device2.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testDeleteRelationWithOtherToDeviceError() throws Exception {
        Device device = createDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = createDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device2.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testDeleteRelations() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME + " from");
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME + " to");

        String urlTo = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );
        String urlFrom = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(urlTo, numOfDevices);
        assertFoundList(urlFrom, numOfDevices);

        String url = String.format("/api/relations?entityId=%s&entityType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(null, mainDevice.getId(), mainDevice.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.RELATIONS_DELETED);

        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlTo, List.class).isEmpty()
        );
        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlFrom, List.class).isEmpty()
        );
    }

    @Test
    public void testFindRelationsByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<>() {}
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    public void testFindRelationsByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<>() {}
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    public void testFindRelationsInfoByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByFrom(relationsInfo);
    }

    @Test
    public void testFindRelationsInfoByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByTo(relationsInfo);
    }

    @Test
    public void testCreateRelationFromTenantToDevice() throws Exception {
        EntityRelation relation = new EntityRelation(tenantId, mainDevice.getId(), "CONTAINS");
        relation = doPost("/api/v2/relation", relation, EntityRelation.class);

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                tenantId, EntityType.TENANT,
                "CONTAINS", mainDevice.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    public void testCreateRelationFromDeviceToTenant() throws Exception {
        EntityRelation relation = new EntityRelation(mainDevice.getId(), tenantId, "CONTAINS");
        relation = doPost("/api/v2/relation", relation, EntityRelation.class);

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", tenantId, EntityType.TENANT
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    public void testSaveAndFindRelationDifferentTenant() throws Exception {
        Device device = createDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        loginDifferentTenant();

        doGet(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", relation.getFrom().getId().toString()))));

        deleteDifferentTenant();
    }

    private Device createDevice(String name) {
        var device = new Device();
        device.setName(name);
        device.setType("default");
        return doPost("/api/device", device, Device.class);
    }

    private ResultActions getRelation(EntityRelation relation) throws Exception {
        return doGet("/api/relation?" +
                "fromId=" + relation.getFrom().getId() +
                "&fromType=" + relation.getFrom().getEntityType() +
                "&relationType=" + relation.getType() +
                "&toId=" + relation.getTo().getId() +
                "&toType=" + relation.getTo().getEntityType());
    }

    private EntityRelation createFromRelation(Device mainDevice, Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }

    private void createDevicesByFrom(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = createDevice(baseName + i);

            EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    private void createDevicesByTo(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = createDevice(baseName + i);
            EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    private void assertFoundRelations(List<EntityRelation> relations, int numOfDevices) {
        Assert.assertNotNull("Relations is not found!", relations);
        Assert.assertEquals("List of found relations is not equal to number of created relations!",
                numOfDevices, relations.size());
    }

    private void assertFoundList(String url, int numOfDevices) throws Exception {
        @SuppressWarnings("unchecked")
        List<EntityRelation> relations = doGet(url, List.class);
        assertFoundRelations(relations, numOfDevices);
    }

    private void assertRelationsInfosByFrom(List<EntityRelationInfo> relationsInfos) {
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong FROM entityId!", mainDevice.getId(), info.getFrom());
            Assert.assertTrue("Wrong FROM name!", info.getToName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }

    private void assertRelationsInfosByTo(List<EntityRelationInfo> relationsInfos) {
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong TO entityId!", mainDevice.getId(), info.getTo());
            Assert.assertTrue("Wrong TO name!", info.getFromName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }

}
