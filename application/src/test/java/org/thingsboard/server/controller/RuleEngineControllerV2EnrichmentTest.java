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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.engine.EnrichEntityDescriptor;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the v2 Rule Engine REST API and its server-authoritative metadata
 * enrichment (tb_acl + tb_user_id).
 */
@DaoSqlTest
public class RuleEngineControllerV2EnrichmentTest extends AbstractControllerTest {

    private static final String RESPONSE_BODY = "{\"response\":\"ok\"}";

    @SpyBean
    private RuleEngineCallService ruleEngineCallService;

    @Test
    public void testV2EnrichmentPopulatesTbAclAndTbUserId() throws Exception {
        loginTenantAdmin();
        mockRestApiCallToRuleEngine(newReplyMsg());

        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{\"k\":\"v\"}"));
        body.setEnrichEntities(List.of(
                new EnrichEntityDescriptor(EntityType.DEVICE, UUID.randomUUID())
        ));

        JsonNode apiResponse = doPostAsyncWithTypedResponse(
                "/api/rule-engine/v2/", JacksonUtil.toString(body),
                new TypeReference<>() {}, status().isOk());
        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);

        TbMsg sent = captureForwardedMsg();

        // Payload, not the wrapper, is forwarded as TbMsg.data.
        assertThat(sent.getData()).isEqualTo("{\"k\":\"v\"}");

        // tb_user_id is the caller's UUID.
        String userId = sent.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY);
        assertThat(userId).isEqualTo(tenantAdminUserId.getId().toString());

        // tb_acl is a parseable JSON array with one entry matching the requested DEVICE.
        String aclJson = sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY);
        assertThat(aclJson).isNotBlank();
        List<EntityAclEntry> acl = JacksonUtil.fromString(aclJson, new TypeReference<List<EntityAclEntry>>() {});
        assertThat(acl).hasSize(1);
        assertThat(acl.get(0).getEntityType()).isEqualTo(EntityType.DEVICE);
        assertThat(acl.get(0).getAllowed()).isNotNull();
        // Tenant admin has at least READ and WRITE on DEVICE resource at role level.
        assertThat(acl.get(0).getAllowed()).contains("READ", "WRITE");
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
                "/api/rule-engine/v2/", JacksonUtil.toString(body),
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
                "/api/rule-engine/v2/", JacksonUtil.toString(body),
                new TypeReference<JsonNode>() {}, status().isOk());

        TbMsg sent = captureForwardedMsg();
        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_ACL_KEY)).isEqualTo("[]");
        assertThat(sent.getMetaData().getValue(TbMsgMetaData.TB_USER_ID_KEY))
                .isEqualTo(tenantAdminUserId.getId().toString());
    }

    @Test
    public void testV2RejectsRequestsExceedingMaxEntities() throws Exception {
        loginTenantAdmin();

        // Default max-entities = 20. Build 21 descriptors.
        List<EnrichEntityDescriptor> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(new EnrichEntityDescriptor(EntityType.DEVICE, UUID.randomUUID()));
        }
        EnrichedRuleEngineRequest body = new EnrichedRuleEngineRequest();
        body.setPayload(JacksonUtil.toJsonNode("{}"));
        body.setEnrichEntities(tooMany);

        doPost("/api/rule-engine/v2/", JacksonUtil.toString(body))
                .andExpect(status().isBadRequest());
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
