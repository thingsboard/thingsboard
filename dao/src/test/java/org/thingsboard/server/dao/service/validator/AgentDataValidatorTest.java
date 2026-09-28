// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AgentDataValidator.class)
@Slf4j
class AgentDataValidatorTest {

    @MockitoBean
    AgentDao agentDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    CustomerDao customerDao;
    @MockitoBean
    SubscriptionService subscriptionService;
    @MockitoBean
    ApiLimitService apiLimitService;
    @Autowired
    AgentDataValidator validator;
    
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    TenantId tenantId2 = TenantId.fromUUID(UUID.fromString("8ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    CustomerId customerId = new CustomerId(UUID.fromString("7ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(true).given(tenantService).tenantExists(tenantId2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "agent1", "1", "test agent", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void testAgentName_thenOK(final String name) {
        Agent agent = validAgent();
        agent.setName(name);
        validator.validateDataImpl(tenantId, agent);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void testAgentName_thenDataValidationException(final String name) {
        Agent agent = validAgent();
        agent.setName(name);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Agent name").containsPattern("Agent [Nn]ame .*");
    }

    @Test
    void testValidateDataImpl_emptyRoutingKey_thenException() {
        Agent agent = validAgent();
        agent.setRoutingKey(null);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        assertThat(exception.getMessage()).contains("routing key");
    }

    @Test
    void testValidateDataImpl_emptySecret_thenException() {
        Agent agent = validAgent();
        agent.setSecret(null);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        assertThat(exception.getMessage()).contains("secret");
    }

    @Test
    void testValidateDataImpl_nullTenantId_thenException() {
        Agent agent = validAgent();
        agent.setTenantId(null);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).containsIgnoringCase("tenant");
    }

    @Test
    void testValidateDataImpl_nonExistentTenant_thenException() {
        TenantId nonExistentTenantId = TenantId.fromUUID(UUID.fromString("1ef79cdf-37a8-4119-b682-2e7ed4e018da"));
        willReturn(false).given(tenantService).tenantExists(nonExistentTenantId);

        Agent agent = validAgent();
        agent.setTenantId(nonExistentTenantId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).contains("non-existent tenant");
    }

    @Test
    void testValidateDataImpl_nullCustomerId_thenSetToNullUuid() {
        Agent agent = validAgent();
        agent.setCustomerId(null);

        validator.validateDataImpl(tenantId, agent);

        assertThat(agent.getCustomerId()).isNotNull();
        assertThat(agent.getCustomerId().getId()).isEqualTo(CustomerId.NULL_UUID);
    }

    @Test
    void testValidateDataImpl_validCustomerId_thenOK() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setTitle("Test Customer");

        willReturn(customer).given(customerDao).findById(eq(tenantId), eq(customerId.getId()));

        Agent agent = validAgent();
        agent.setCustomerId(customerId);

        validator.validateDataImpl(tenantId, agent);
    }

    @Test
    void testValidateDataImpl_nonExistentCustomer_thenException() {
        willReturn(null).given(customerDao).findById(eq(tenantId), any(UUID.class));

        Agent agent = validAgent();
        agent.setCustomerId(customerId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).contains("non-existent customer");
    }

    @Test
    void testValidateDataImpl_customerFromDifferentTenant_thenException() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId2); // Different tenant
        customer.setTitle("Test Customer");

        willReturn(customer).given(customerDao).findById(eq(tenantId), eq(customerId.getId()));

        Agent agent = validAgent();
        agent.setCustomerId(customerId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).contains("different tenant");
    }

    @Test
    void testValidateCreate_underLicenseLimit_thenOK() {
        willReturn(true).given(apiLimitService).checkEntitiesLimit(tenantId, EntityType.AGENT);
        Agent agent = validAgent();

        validator.validateCreate(tenantId, agent);

        verify(subscriptionService).createAgentAllowed(agent.getTenantId());
        verify(apiLimitService).checkEntitiesLimit(tenantId, EntityType.AGENT);
    }

    @Test
    void testValidateCreate_licenseLimitReached_thenException() {
        willThrow(new SubscriptionException("Maximum allowed agents limit reached!",
                SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.AGENT_COUNT, 5L))
                .given(subscriptionService).createAgentAllowed(tenantId);

        Agent agent = validAgent();

        SubscriptionException exception = Assertions.assertThrows(SubscriptionException.class,
                () -> validator.validateCreate(tenantId, agent));
        assertThat(exception.getErrorCode()).isEqualTo(SubscriptionErrorCode.LIMIT_REACHED);
        assertThat(exception.getEntry()).isEqualTo(SubscriptionEntry.AGENT_COUNT);
        assertThat(exception.getValue().get("value").asLong()).isEqualTo(5L);
    }

    private Agent validAgent() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Test Agent");
        agent.setRoutingKey(UUID.randomUUID().toString());
        agent.setSecret(UUID.randomUUID().toString());
        return agent;
    }

    @Test
    void testValidateCreate_underLimit_thenOK() {
        willReturn(true).given(apiLimitService).checkEntitiesLimit(tenantId, EntityType.AGENT);

        assertThatNoException().isThrownBy(() -> validator.validateCreate(tenantId, validAgent()));
    }

    @Test
    void testValidateCreate_limitReached_thenEntitiesLimitExceeded() {
        long limit = 5;
        willReturn(false).given(apiLimitService).checkEntitiesLimit(tenantId, EntityType.AGENT);
        willReturn(limit).given(apiLimitService).getLimit(eq(tenantId), any());

        assertThatThrownBy(() -> validator.validateCreate(tenantId, validAgent()))
                .isInstanceOfSatisfying(EntitiesLimitExceededException.class, ex -> {
                    assertThat(ex.getTenantId()).isEqualTo(tenantId);
                    assertThat(ex.getEntityType()).isEqualTo(EntityType.AGENT);
                    assertThat(ex.getLimit()).isEqualTo(limit);
                });
    }

    @Test
    void testValidateUpdate_existingAgent_thenReturnOldAgent() {
        UUID agentUuid = UUID.randomUUID();
        AgentId agentId = new AgentId(agentUuid);

        Agent oldAgent = new Agent();
        oldAgent.setId(agentId);
        oldAgent.setName("Old Agent Name");
        oldAgent.setTenantId(tenantId);

        willReturn(oldAgent).given(agentDao).findById(eq(tenantId), eq(agentUuid));

        Agent newAgent = new Agent();
        newAgent.setId(agentId);
        newAgent.setName("New Agent Name");
        newAgent.setTenantId(tenantId);

        Agent result = validator.validateUpdate(tenantId, newAgent);

        assertThat(result).isEqualTo(oldAgent);
    }

    @Test
    void testValidateUpdate_nonExistentAgent_thenException() {
        UUID agentUuid = UUID.randomUUID();
        AgentId agentId = new AgentId(agentUuid);

        willReturn(null).given(agentDao).findById(eq(tenantId), eq(agentUuid));

        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setName("Test Agent");
        agent.setTenantId(tenantId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, agent));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).contains("non existing agent");
    }
}
