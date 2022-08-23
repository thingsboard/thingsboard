/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractControllerTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@EnableWebSocket
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class AbstractControllerTest extends AbstractNotifyEntityTest {

    public static final String WS_URL = "ws://localhost:";

    @LocalServerPort
    protected int wsPort;

    private TbTestWebSocketClient wsClient; // lazy

    public TbTestWebSocketClient getWsClient() {
        if (wsClient == null) {
            synchronized (this) {
                try {
                    if (wsClient == null) {
                        wsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return wsClient;
    }

    @Before
    public void beforeWsTest() throws Exception {
        // placeholder
    }

    @After
    public void afterWsTest() throws Exception {
        if (wsClient != null) {
            wsClient.close();
        }
    }

    private TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        TbTestWebSocketClient wsClient = new TbTestWebSocketClient(new URI(WS_URL + wsPort + "/api/ws/plugins/telemetry?token=" + token));
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return wsClient;
    }


    public <T, E> void testDeleteEntity_ExistsRelationToEntity_Error_RestoreRelationToEntity_DeleteRelation_DeleteEntity_Ok(
            TenantId tenantId, CustomerId customerId, Class<T> clazzTestEntity, EntityId testEntityId, T savedTestEntity,
            EntityId entityIdFrom, E entityFromWithoutEntityTo, String urlGetTestEntity, String urlDeleteTestEntity,
            String urlUpdateEntityFrom, String entityTestNameClass, String name, String entityTestMsgNotDelete, int cntOtherEntity
    ) throws Exception {

        Map<EntityId, HasName> entities = createEntities(entityTestNameClass + " " + name, cntOtherEntity);

        Alarm savedAlarmForTestEntity = getAlarmWithControlPropagated(tenantId, customerId, "Alarm by " + name, entityTestNameClass, testEntityId);

        // Create Alarms for other entity
        entities.forEach((k, v) -> getAlarmWithControlPropagated(tenantId, customerId, "Alarm by " + v.getName() + " " + name, entityTestNameClass, k));

        entities.put(testEntityId, (HasName)savedTestEntity);

        // Create entityRelations: from -> entityFrom, to -> entities
        String typeRelation = EntityRelation.CONTAINS_TYPE;
        Map<EntityRelation, String> entityRelations = createEntityRelations(entityIdFrom, entities, typeRelation);
        Optional<Map.Entry<EntityRelation, String>> relationMapTestEntityTo = entityRelations.entrySet().stream().filter(e -> e.getKey().getTo().equals(testEntityId)).findFirst();
        assertTrue("TestEntityRelation is found after " + entityTestNameClass + " deletion 'success'!", relationMapTestEntityTo.isPresent());
        String urlRelationTestEntityTo = relationMapTestEntityTo.get().getValue();

        String testEntityIdStr = testEntityId.getId().toString();
        doDelete(urlDeleteTestEntity + testEntityIdStr)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(entityTestMsgNotDelete)));

        savedTestEntity = doGet(urlGetTestEntity + testEntityIdStr, clazzTestEntity);
        assertNotNull(entityTestNameClass + " is not found!", savedTestEntity);
        assertEquals(entityTestNameClass + " after delete error is not equals origin!", savedTestEntity, savedTestEntity);


        String urlEntityFroms = String.format("/api/relations?fromId=%s&fromType=%s",
                entityIdFrom.getId(), entityIdFrom.getEntityType());
        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(urlEntityFroms, JsonNode.class), new TypeReference<>() {
                });
        int numOfRelations = entityRelations.size();
        assertNotNull("Relations is not found!", relationsInfos);
        assertEquals("List of found relations is not equal to number of created relations!",
                numOfRelations, relationsInfos.size());
        EntityId expectTestEntityId = testEntityId;
        Optional<EntityRelationInfo> expectTestEntityRelationInfo = relationsInfos.stream().filter(k -> k.getTo().equals(expectTestEntityId)).findFirst();
        assertTrue("TestEntityRelation is not found after " + entityTestNameClass + " deletion 'bad request'!", expectTestEntityRelationInfo.isPresent());
        String expectTestEntityRelationToIdStr = expectTestEntityRelationInfo.get().getTo().getId().toString();

        AlarmOperationResult afterErrorDeleteTestEntityAlarmOperationResult = getAlarmOperationResult(savedAlarmForTestEntity);
        assertTrue("AfterErrorDelete" + entityTestNameClass + "AlarmOperationResult is not success!", afterErrorDeleteTestEntityAlarmOperationResult.isSuccessful());
        assertTrue("List of propagatedEntities is not equal to number of created propagatedEntities!",
                afterErrorDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList().size() > 0);
        assertTrue(entityTestNameClass + "Id in propagatedEntities is not equal saved" + entityTestNameClass + "Id!",
                afterErrorDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(testEntityId))
                        .findFirst()
                        .isPresent());

        doPost(urlUpdateEntityFrom, entityFromWithoutEntityTo)
                .andExpect(status().isOk());

        doDelete(urlDeleteTestEntity + testEntityIdStr)
                .andExpect(status().isOk());
        doGet(urlGetTestEntity + testEntityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(entityTestNameClass, testEntityIdStr))));

        doGet(urlRelationTestEntityTo)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(entityTestNameClass, expectTestEntityRelationToIdStr))));
        relationsInfos =
                JacksonUtil.convertValue(doGet(urlEntityFroms, JsonNode.class), new TypeReference<>() { });
        assertNotNull("Relations is not found!", relationsInfos);
        assertEquals("List of found relations is not equal to number of relations left!",
                numOfRelations - 1, relationsInfos.size());
        expectTestEntityRelationInfo = relationsInfos.stream().filter(k -> k.getTo().equals(expectTestEntityId)).findFirst();
        assertTrue("TestEntityRelation is found after " + entityTestNameClass + " deletion 'success'!", expectTestEntityRelationInfo.isEmpty());

        AlarmOperationResult afterSuccessDeleteTestEntityAlarmOperationResult = getAlarmOperationResult(savedAlarmForTestEntity);
        assertTrue("AfterSuccessDelete" + entityTestNameClass + "AlarmOperationResult is not success!", afterSuccessDeleteTestEntityAlarmOperationResult.isSuccessful());
        assertTrue(entityTestNameClass + "Id in propagatedEntities is equal saved" + entityTestNameClass + "Id!",
                afterSuccessDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(testEntityId))
                        .findFirst()
                        .isEmpty());
    }

    private EntityRelation createEntityRelation(EntityId entityIdFrom, EntityId entityIdTo, String url, String typeRelation) throws Exception {
        EntityRelation relation = new EntityRelation(entityIdFrom, entityIdTo, typeRelation);
        doPost("/api/relation", relation).andExpect(status().isOk());

        EntityRelation foundRelation = doGet(url, EntityRelation.class);
        Assert.assertNotNull("Relation is not found!", foundRelation);
        assertEquals("Found relation is not equals origin!", relation, foundRelation);

        return foundRelation;
    }

    private Map<EntityId, HasName> createEntities(String name, int cntOtherEntity) throws Exception {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
        List<ListenableFuture<Device>> futures = new ArrayList<>(cntOtherEntity);
        for (int i = 0; i < cntOtherEntity; i++) {
            Device device = new Device();
            device.setName(name + i);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);
        Map<EntityId, Device> deviceMap = Maps.uniqueIndex(devices, Device::getId)
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));

        return deviceMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));
    }

    private Map<EntityRelation, String> createEntityRelations(EntityId entityIdFrom, Map<EntityId, HasName> entityTos, String typeRelation) {
        Map<EntityRelation, String> entityRelations = new HashMap<>();
        entityTos.keySet().forEach(k -> {
            try {
                String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                        entityIdFrom.getId(), entityIdFrom.getEntityType(),
                        typeRelation,
                        k.getId(), k.getEntityType()
                );
                EntityRelation relation = createEntityRelation(entityIdFrom, k, url, typeRelation);
                entityRelations.put(relation, url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return entityRelations;
    }

    private Alarm getAlarmWithControlPropagated(TenantId tenantId, CustomerId customerId, String name, String entityNameClass, EntityId entityId) {
        Alarm alarm = createAlarmWithPropagate(tenantId, customerId, name, entityId);
        AlarmOperationResult alarmOperationResult = getAlarmOperationResult(alarm);

        assertTrue("AlarmOperationResult is not success!", alarmOperationResult.isSuccessful());
        assertTrue("List of propagatedEntities is not equal to number of created propagatedEntities!",
                alarmOperationResult.getPropagatedEntitiesList().size() > 0);
        assertTrue(entityNameClass + "Id in propagatedEntities is not equal saved" + entityNameClass + "Id!",
                alarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(entityId))
                        .findFirst()
                        .isPresent());
                assertTrue("AlarmOperationResult is not success!", alarmOperationResult.isSuccessful());
        assertTrue("List of propagatedEntities is not equal to number of created propagatedEntities!",
                alarmOperationResult.getPropagatedEntitiesList().size() > 0);
        assertTrue(entityNameClass + "Id in propagatedEntities is not equal saved" + entityNameClass + "Id!",
                alarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(entityId))
                        .findFirst()
                        .isPresent());
        Alarm savedAlarm = alarmOperationResult.getAlarm();
        assertNotNull("SavedAlarm is not found!", savedAlarm);

        return savedAlarm;
    }
}
