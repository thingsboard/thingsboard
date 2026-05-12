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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.engine.EntityAclEntry;
import org.thingsboard.server.common.data.rule.engine.RuleEngineV2Request;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = "rule_engine.acl.max_entities=5")
public class RuleEngineControllerV2EnrichmentTest extends AbstractControllerTest {

    private static final int MAX_ACL_ENTITIES = 5;
    private static final String URL = "/api/rule-engine/v2";
    private static final String RESPONSE_BODY = "{\"response\":\"ok\"}";

    @SpyBean
    private RuleEngineCallService ruleEngineCallService;

    @SpyBean
    private EntityService entityService;

    @Test
    public void testV2TenantAdminGetsFullAclOnOwnDevice() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("dev-tenant", "tok-1");

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(device.getId()));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityId()).isEqualTo(device.getId());
        assertThat(acl.get(0).getEntityId().getEntityType()).isEqualTo(EntityType.DEVICE);
        assertThat(acl.get(0).getAllowed()).contains("READ", "WRITE", "DELETE", "WRITE_TELEMETRY");
    }

    @Test
    public void testV2CustomerUserGetsAclOnOwnDevice() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("dev-customer", "tok-2");
        assignDeviceToCustomer(device.getId(), customerId);
        loginCustomerUser();

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(device.getId()));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getAllowed()).contains("READ", "WRITE", "READ_TELEMETRY", "WRITE_TELEMETRY");
    }

    @Test
    public void testV2CustomerUserCannotWriteForeignDevice() throws Exception {
        loginDifferentCustomer();
        loginTenantAdmin();
        Device foreignDevice = createDevice("dev-foreign", "tok-3");
        assignDeviceToCustomer(foreignDevice.getId(), differentCustomerId);
        loginCustomerUser();

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(foreignDevice.getId()));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityId()).isEqualTo(foreignDevice.getId());
        // Platform allows CLAIM_DEVICES on any tenant device by design; everything else must be denied.
        assertThat(acl.get(0).getAllowed())
                .doesNotContain("READ", "WRITE", "READ_TELEMETRY", "WRITE_TELEMETRY",
                        "READ_ATTRIBUTES", "WRITE_ATTRIBUTES", "READ_CREDENTIALS", "RPC_CALL");
    }

    @Test
    public void testV2TwoCustomersSeeOnlyTheirOwnDevices() throws Exception {
        loginDifferentCustomer();
        loginTenantAdmin();
        Device deviceA = createDevice("dev-A", "tok-A");
        Device deviceB = createDevice("dev-B", "tok-B");
        assignDeviceToCustomer(deviceA.getId(), customerId);
        assignDeviceToCustomer(deviceB.getId(), differentCustomerId);

        // Customer 1 (own A, foreign B)
        loginCustomerUser();
        RuleEngineV2Request req1 = baseRequest();
        req1.setAclEntities(List.of(deviceA.getId(), deviceB.getId()));
        TbMsg captured1 = doRequestAndCapture(req1, tenantId);
        List<EntityAclEntry> acl1 = parseAcl(captured1);
        assertThat(acl1).hasSize(2);
        assertThat(acl1.get(0).getEntityId()).isEqualTo(deviceA.getId());
        assertThat(acl1.get(0).getAllowed()).contains("WRITE", "READ_TELEMETRY");
        assertThat(acl1.get(1).getEntityId()).isEqualTo(deviceB.getId());
        assertThat(acl1.get(1).getAllowed()).doesNotContain("WRITE", "READ", "READ_TELEMETRY");

        // Customer 2 (foreign A, own B).
        loginDifferentCustomer();
        RuleEngineV2Request req2 = baseRequest();
        req2.setAclEntities(List.of(deviceA.getId(), deviceB.getId()));
        TbMsg captured2 = doRequestAndCapture(req2, tenantId);
        List<EntityAclEntry> acl2 = parseAcl(captured2);
        assertThat(acl2).hasSize(2);
        assertThat(acl2.get(0).getEntityId()).isEqualTo(deviceA.getId());
        assertThat(acl2.get(0).getAllowed()).doesNotContain("WRITE", "READ", "READ_TELEMETRY");
        assertThat(acl2.get(1).getEntityId()).isEqualTo(deviceB.getId());
        assertThat(acl2.get(1).getAllowed()).contains("WRITE", "READ_TELEMETRY");
    }

    @Test
    public void testV2NullAclEntitiesProducesEmptyAcl() throws Exception {
        loginTenantAdmin();

        RuleEngineV2Request request = baseRequest();
        // aclEntities left null
        TbMsg captured = doRequestAndCapture(request, tenantId);

        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_ACL_SNAPSHOT_KEY)).isEqualTo("[]");
        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
    }

    @Test
    public void testV2EmptyAclEntitiesListProducesEmptyAcl() throws Exception {
        loginTenantAdmin();

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of());
        TbMsg captured = doRequestAndCapture(request, tenantId);

        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_ACL_SNAPSHOT_KEY)).isEqualTo("[]");
        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
    }

    @Test
    public void testV2DuplicateEntitiesPreservedInOutputAndDedupedInWork() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("dev-dup", "tok-dup");
        Device other = createDevice("dev-other", "tok-other");

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(device.getId(), device.getId(), other.getId()));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(3);
        assertThat(acl.get(0).getEntityId()).isEqualTo(device.getId());
        assertThat(acl.get(1).getEntityId()).isEqualTo(device.getId());
        assertThat(acl.get(2).getEntityId()).isEqualTo(other.getId());

        // Dedup: the duplicated id triggers one fetchEntity, not two.
        verify(entityService, times(1)).fetchEntity(eq(tenantId), eq(device.getId()));
        verify(entityService, times(1)).fetchEntity(eq(tenantId), eq(other.getId()));
    }

    @Test
    public void testV2RejectsRequestExceedingMaxEntities() throws Exception {
        loginTenantAdmin();
        // bound is set via @TestPropertySource — test is independent of production default.
        List<EntityId> tooMany = new ArrayList<>();
        for (int i = 0; i < MAX_ACL_ENTITIES + 1; i++) {
            tooMany.add(new DeviceId(UUID.randomUUID()));
        }

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(tooMany);

        doPost(URL, request).andExpect(status().isBadRequest());
    }

    @Test
    public void testV2UnmappedEntityTypeProducesEmptyAcl() throws Exception {
        loginTenantAdmin();
        // RULE_NODE has no Resource mapping — Resource.of throws, entry resolves to allowed=[].
        RuleNodeId fakeRuleNode = new RuleNodeId(UUID.randomUUID());

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(fakeRuleNode));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityId().getEntityType()).isEqualTo(EntityType.RULE_NODE);
        assertThat(acl.get(0).getAllowed()).isEmpty();
    }

    @Test
    public void testV2NonexistentDeviceProducesEmptyAcl() throws Exception {
        loginTenantAdmin();
        DeviceId ghost = new DeviceId(UUID.randomUUID());

        RuleEngineV2Request request = baseRequest();
        request.setAclEntities(List.of(ghost));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        List<EntityAclEntry> acl = parseAcl(captured);
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityId()).isEqualTo(ghost);
        assertThat(acl.get(0).getAllowed()).isEmpty();
    }

    @Test
    public void testV2PayloadCannotInjectAclMetadata() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("dev-inj", "tok-inj");

        RuleEngineV2Request request = baseRequest();
        request.setPayload(JacksonUtil.toJsonNode("{\"" + TbMsgMetaData.TB_ACL_SNAPSHOT_KEY + "\":\"attack\",\"" +
                TbMsgMetaData.TB_USER_ID_KEY + "\":\"intruder\"}"));
        request.setAclEntities(List.of(device.getId()));

        TbMsg captured = doRequestAndCapture(request, tenantId);

        // Server-computed values, not the attacker's.
        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_ACL_SNAPSHOT_KEY)).contains("\"entityType\":\"DEVICE\"");
        assertThat(captured.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
    }

    @Test
    public void testV2RequiresPayload() throws Exception {
        loginTenantAdmin();
        RuleEngineV2Request request = new RuleEngineV2Request();
        // payload deliberately not set — the v2 contract now requires it.

        doPost(URL, request).andExpect(status().isBadRequest());
    }

    @Test
    public void testV2ForwardsRestApiRequestTypeAndHonorsBodyTimeout() throws Exception {
        loginTenantAdmin();

        RuleEngineV2Request request = baseRequest();
        request.setTimeout(2000);

        long beforeMs = System.currentTimeMillis();
        TbMsg captured = doRequestAndCapture(request, tenantId);
        long afterMs = System.currentTimeMillis();

        assertThat(captured.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        long expirationTime = Long.parseLong(captured.getMetaData().getValue("expirationTime"));
        assertThat(expirationTime).isBetween(beforeMs + 2000, afterMs + 2000);
    }

    private RuleEngineV2Request baseRequest() {
        RuleEngineV2Request request = new RuleEngineV2Request();
        request.setPayload(JacksonUtil.toJsonNode("{\"k\":\"v\"}"));
        return request;
    }

    private TbMsg doRequestAndCapture(RuleEngineV2Request request, TenantId expectedTenantId) throws Exception {
        TbMsg responseMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(currentUserId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        doPostAsyncWithTypedResponse(URL, request, new TypeReference<JsonNode>() {
        }, status().isOk());

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService, atLeastOnce()).processRestApiCallToRuleEngine(eq(expectedTenantId),
                any(UUID.class), captor.capture(), anyBoolean(), any(Consumer.class));
        List<TbMsg> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }

    private List<EntityAclEntry> parseAcl(TbMsg msg) {
        String acl = msg.getMetaData().getValue(TbMsgMetaData.TB_ACL_SNAPSHOT_KEY);
        return JacksonUtil.fromString(acl, new TypeReference<List<EntityAclEntry>>() {
        });
    }

    private void mockRestApiCallToRuleEngine(TbMsg responseMsg) {
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(4);
            consumer.accept(responseMsg);
            return null;
        }).when(ruleEngineCallService).processRestApiCallToRuleEngine(any(TenantId.class), any(UUID.class),
                any(TbMsg.class), anyBoolean(), any(Consumer.class));
    }

}
