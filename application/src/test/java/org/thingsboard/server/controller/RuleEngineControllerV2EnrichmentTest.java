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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.engine.EnrichedRuleEngineRequest;
import org.thingsboard.server.common.data.rule.engine.EntityAclEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the v2 Rule Engine REST API and its server-authoritative metadata
 * enrichment (tb_acl + tb_user_id). All routing parameters live in the request
 * body — there is a single endpoint at {@code POST /api/ruleEngine/v2/}.
 */
@DaoSqlTest
public class RuleEngineControllerV2EnrichmentTest extends AbstractControllerTest {

    private static final String V2_URL = "/api/rule-engine/v2/";
    private static final String RESPONSE_BODY = "{\"response\":\"ok\"}";

    @SpyBean
    private RuleEngineCallService ruleEngineCallService;

    @Test
    public void testV2EnrichmentPopulatesTbAclAndTbUserId() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{\"k\":\"v\"}"));
        body.setEnrichEntities(List.of(new DeviceId(UUID.randomUUID())));

        JsonNode apiResponse = doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<>() {}, status().isOk());
        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);

        TbMsg sent = captureForwardedMsg();

        // Payload, not the wrapper, is forwarded as TbMsg.data.
        assertThat(sent.getData()).isEqualTo("{\"k\":\"v\"}");

        // Default messageType — REST_API_REQUEST — is preserved when body omits it.
        assertThat(sent.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());

        // tb_user_id is the caller's UUID.
        String userId = sent.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY);
        assertThat(userId).isEqualTo(tenantAdminUserId.getId().toString());

        // tb_acl is a parseable JSON array with one entry matching the requested DEVICE.
        String aclJson = sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY);
        assertThat(aclJson).isNotBlank();
        List<EntityAclEntry> acl = JacksonUtil.fromString(aclJson, new TypeReference<List<EntityAclEntry>>() {});
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityType()).isEqualTo(EntityType.DEVICE);
        assertThat(acl.get(0).getRoleAllowed()).isNotNull();
        // Tenant admin has at least READ and WRITE on DEVICE resource at role level.
        assertThat(acl.get(0).getRoleAllowed()).contains("READ", "WRITE");
    }

    @Test
    public void testV2CallerCannotOverrideTbAclOrTbUserId() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        // Even if the caller tries to embed reserved keys inside the JSON payload,
        // they live in TbMsg.data — not metadata — and the server still overwrites metadata.
        String evilPayload = "{\"tb_acl\":\"HACKED\",\"tb_user_id\":\"00000000-0000-0000-0000-000000000000\"}";
        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode(evilPayload));
        body.setEnrichEntities(List.of());

        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();

        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY)).isEqualTo("[]");
        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString())
                .isNotEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void testV2EmptyEnrichListProducesEmptyAclArray() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        // no enrichEntities field at all

        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY)).isEqualTo("[]");
        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
    }

    @Test
    public void testV2RejectsRequestWithoutPayload() throws Exception {
        loginTenantAdmin();

        // payload is required; an empty body returns 400.
        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();

        // Pass the typed object (not its serialized String) so Java picks
        // doPost(String, T, String...) — passing a String would land it in the
        // varargs of doPost(String, String...) and fail params validation.
        doPost(V2_URL, body)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testV2UnmappedEntityTypeProducesEmptyRoleAllowed() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        // RULE_NODE has no entry in the Resource enum — rule nodes are managed through
        // their parent RuleChain rather than as standalone permissioned entities, so
        // Resource.of(RULE_NODE) throws and the ACL entry must come back empty.
        body.setEnrichEntities(List.of(new RuleNodeId(UUID.randomUUID())));

        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        List<EntityAclEntry> acl = JacksonUtil.fromString(
                sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY),
                new TypeReference<List<EntityAclEntry>>() {});
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityType()).isEqualTo(EntityType.RULE_NODE);
        assertThat(acl.get(0).getRoleAllowed())
                .as("unmapped EntityType must yield an empty roleAllowed list").isEmpty();
    }

    @Test
    public void testV2RejectsRequestsExceedingMaxEntities() throws Exception {
        loginTenantAdmin();

        // Default max-entities = 20. Build 21 descriptors.
        List<EntityId> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(new DeviceId(UUID.randomUUID()));
        }
        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        body.setEnrichEntities(tooMany);

        // Pass the typed object (see comment in testV2RejectsRequestWithoutPayload).
        doPost(V2_URL, body)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testV2CustomerUserGetsRoleScopedAcl() throws Exception {
        loginCustomerUser();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        body.setEnrichEntities(List.of(new DeviceId(UUID.randomUUID())));
        // Originator defaults to current user; customer user can WRITE on themselves.

        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        List<EntityAclEntry> acl = JacksonUtil.fromString(
                sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY),
                new TypeReference<List<EntityAclEntry>>() {});
        assertThat(acl).hasSize(1);

        // Customer user role on DEVICE grants READ/WRITE plus the *_ATTRIBUTES/_TELEMETRY
        // and RPC_CALL/CLAIM_DEVICES variants — see CustomerUserPermissions.
        List<String> roleAllowed = acl.get(0).getRoleAllowed();
        assertThat(roleAllowed).contains("READ", "WRITE");
        // …but NOT CREATE or DELETE — those are tenant-admin-only on DEVICE.
        // Without role-scoping the ACL feature would be silently broken.
        assertThat(roleAllowed).doesNotContain("CREATE", "DELETE");
    }

    @Test
    public void testV2AclJsonUsesRoleAllowedKey() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        body.setEnrichEntities(List.of(new DeviceId(UUID.randomUUID())));

        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        // Parse as raw JsonNode — rule-chain JS scripts use JSON.parse and access
        // entries by literal key name, so the wire-level key matters independently
        // of any Java-side getter renaming.
        JsonNode acl = JacksonUtil.toJsonNode(sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY));
        assertThat(acl.isArray()).isTrue();
        assertThat(acl).hasSize(1);
        JsonNode entry = acl.get(0);
        assertThat(entry.has("entityType")).isTrue();
        assertThat(entry.has("entityId")).isTrue();
        assertThat(entry.has("roleAllowed"))
                .as("rule-chain consumers expect 'roleAllowed' as the JSON key").isTrue();
        assertThat(entry.has("allowed"))
                .as("'allowed' would be a regression — that name is reserved for misleading per-entity reads").isFalse();
    }

    @Test
    public void testV2OriginatorDefaultsToCallerAndIsHonoredFromBody() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        // Case 1: no originator → defaults to current user (tenant admin).
        EnrichedRuleEngineRequest noOriginator = new EnrichedRuleEngineRequest();
        noOriginator.setPayload(JacksonUtil.toJsonNode("{}"));
        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(noOriginator),
                new TypeReference<JsonNode>() {}, status().isOk());

        // Case 2: explicit originator = customerUserId; tenant admin can WRITE on it.
        EnrichedRuleEngineRequest explicitOriginator = new EnrichedRuleEngineRequest();
        explicitOriginator.setPayload(JacksonUtil.toJsonNode("{}"));
        explicitOriginator.setOriginator(customerUserId);
        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(explicitOriginator),
                new TypeReference<JsonNode>() {}, status().isOk());

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService, times(2)).processRestApiCallToRuleEngine(
                any(TenantId.class), any(UUID.class), captor.capture(), anyBoolean(), any(Consumer.class));
        List<TbMsg> sent = captor.getAllValues();

        assertThat(sent.get(0).getOriginator()).isEqualTo(tenantAdminUserId);
        assertThat(sent.get(1).getOriginator()).isEqualTo(customerUserId);
    }

    @Test
    public void testV2RespectsBodyTimeoutAndMessageType() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        body.setMessageType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        body.setTimeout(2000);

        long beforeMs = System.currentTimeMillis();
        doPostAsyncWithTypedResponse(
                V2_URL, JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        assertThat(sent.getType()).isEqualTo(TbMsgType.POST_TELEMETRY_REQUEST.name());

        // expirationTime metadata is now + body timeout (2s); window allows for slow CI
        // and minor clock drift between beforeMs and the controller's expTime sample.
        long expirationTime = Long.parseLong(sent.getMetaData().getValue("expirationTime"));
        assertThat(expirationTime - beforeMs).isBetween(1500L, 12000L);
    }

    // ---------------------------------------------------------------
    //  helpers (mirroring RuleEngineControllerTest patterns)
    // ---------------------------------------------------------------

    private TbMsg newReplyMsg() {
        return TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(tenantAdminUserId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
    }

    private void mockRestApiCallToRuleEngine(TbMsg responseMsg) {
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(4);
            consumer.accept(responseMsg);
            return null;
        }).when(ruleEngineCallService).processRestApiCallToRuleEngine(
                any(TenantId.class), any(UUID.class), any(TbMsg.class), anyBoolean(), any(Consumer.class));
    }

    private TbMsg captureForwardedMsg() {
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(
                any(TenantId.class), any(UUID.class), captor.capture(), anyBoolean(), any(Consumer.class));
        return captor.getValue();
    }

}
