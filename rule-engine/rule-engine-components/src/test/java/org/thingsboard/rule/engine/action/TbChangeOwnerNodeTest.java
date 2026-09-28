// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbChangeOwnerNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.CUSTOMER);
    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(", "));

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("d369bbbf-4b21-4ee4-aa6a-afe0073c238e"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48bc5450-a122-498b-9a88-90438e560cbb"));
    private final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    private TbChangeOwnerNodeConfiguration config;
    private TbChangeOwnerNode node;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private CustomerService customerServiceMock;

    @BeforeEach
    public void setUp() {
        config = new TbChangeOwnerNodeConfiguration().defaultConfiguration();
        node = spy(new TbChangeOwnerNode());
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getOwnerNamePattern()).isNull();
        assertThat(config.getOwnerType()).isEqualTo(EntityType.TENANT);
        assertThat(config.isCreateOwnerIfNotExists()).isFalse();
        assertThat(config.isCreateOwnerOnOriginatorLevel()).isFalse();
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenChangeDeviceOwnerToCustomer() throws Exception {
        CustomerId customerId = new CustomerId(UUID.fromString("a23cccb2-ea61-4138-ac5b-1b89b13d0cd2"));
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new Customer(customerId))));
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("ownerName", "Test Customer")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(TENANT_ID, customerId, DEVICE_ID);
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDeviceOwnerCustomer_whenOnMsg_thenChangeDeviceOwnerToTenant() throws Exception {
        config.setOwnerType(EntityType.TENANT);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any()))
                .thenReturn(new CustomerId(UUID.fromString("5f59a446-37d2-42b5-8e1e-5ea39363353b")));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(TENANT_ID, TENANT_ID, DEVICE_ID);
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenRemainsOwnerTenant() throws Exception {
        config.setOwnerType(EntityType.TENANT);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDeviceOwnerCustomerAndCreateOwnerIfNotExistsIsTrueAndCreateOwnerOnOriginatorLevelIsTrue_whenOnMsg_thenChangeOwnerToCustomerOriginatorLevel() throws Exception {
        CustomerId ownerId = new CustomerId(UUID.fromString("07c86754-bd6c-4b56-bd8d-df0e21ffcf9b"));
        CustomerId newCustomerId = new CustomerId(UUID.fromString("04ce4d31-e5f4-4925-a51b-48c6a42aca58"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(ownerId);
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        config.setCreateOwnerIfNotExists(true);
        config.setCreateOwnerOnOriginatorLevel(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        TbMsg createdCustomerMsg = TbMsg.newMsg()
                .type(TbMsgType.ENTITY_CREATED)
                .originator(newCustomerId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));
        when(customerServiceMock.saveCustomer(any())).thenReturn(new Customer(newCustomerId));
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(ownerId);
        when(ctxMock.getSelfId()).thenReturn(new RuleNodeId(UUID.fromString("af291c37-47d9-4cfa-89a7-27bfdd6aea6c")));
        when(ctxMock.customerCreatedMsg(any(), any())).thenReturn(createdCustomerMsg);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("ownerName", "Test Customer")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        Customer newCustomer = new Customer();
        newCustomer.setTitle("Test Customer");
        newCustomer.setTenantId(TENANT_ID);
        newCustomer.setOwnerId(ownerId);
        verify(customerServiceMock).saveCustomer(newCustomer);
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(TENANT_ID, newCustomerId, DEVICE_ID);
        verify(ctxMock).enqueue(eq(createdCustomerMsg), any(Runnable.class), any(Consumer.class));
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDeviceOwnerCustomerAndCreateOwnerIfNotExistsIsTrueAndFailedToCreateCustomer_whenOnMsg_thenTellFailure() throws Exception {
        CustomerId ownerId = new CustomerId(UUID.fromString("07c86754-bd6c-4b56-bd8d-df0e21ffcf9b"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(ownerId);
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        config.setCreateOwnerIfNotExists(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));
        doAnswer(invocation -> {
            throw new DataValidationException("Exception during saving customer");
        }).when(customerServiceMock).saveCustomer(any());
        when(customerServiceMock.findCustomerByTenantIdAndTitle(any(), any())).thenReturn(Optional.empty());

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("ownerName", "Test Customer")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to create customer with title 'Test Customer'");
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        Customer newCustomer = new Customer();
        newCustomer.setTitle("Test Customer");
        newCustomer.setTenantId(TENANT_ID);
        verify(customerServiceMock).saveCustomer(newCustomer);
        verify(customerServiceMock).findCustomerByTenantIdAndTitle(TENANT_ID, "Test Customer");
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDeviceOwnerCustomerAndCustomerDoesntExistAndCreateOwnerIfNotExistsIsFalse_whenOnMsg_thenTellFailure() throws Exception {
        CustomerId ownerId = new CustomerId(UUID.fromString("07c86754-bd6c-4b56-bd8d-df0e21ffcf9b"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(ownerId);
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("ownerName", "Test Customer")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage("Customer with title 'Test Customer' doesn't exist!");
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @Test
    public void givenOwnerTypeNull_whenInit_thenThrowsException() {
        config.setOwnerType(null);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Owner type should be specified!");
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void givenConfigAnyOwnerType_whenInit_thenVerify(EntityType ownerType) {
        config.setOwnerType(ownerType);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        if (!supportedEntityTypes.contains(ownerType)) {
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Unsupported owner type '" + ownerType +
                            "'! Only " + supportedEntityTypesStr + " types are allowed.");
        }
        if (EntityType.CUSTOMER.equals(ownerType)) {
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Owner name should be specified!");
        }
        if (EntityType.TENANT.equals(ownerType)) {
            assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
        }
    }

    @Test
    public void givenCreateOwnerIfNotExistsIsFalseAndCreateOwnerOnOriginatorLevelIsTrue_whenOnMsg_thenDoesNotCreateCustomer() throws TbNodeException, ExecutionException, InterruptedException, ThingsboardException {
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("Test Customer");
        config.setCreateOwnerIfNotExists(false);
        config.setCreateOwnerOnOriginatorLevel(true);

        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(customerServiceMock, never()).saveCustomer(any());
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        verify(peContextMock, never()).changeEntityOwner(any(TenantId.class), any(EntityId.class), any(EntityId.class));
    }

    @Test
    public void givenCustomerExistAndCreateOwnerIfNotExistsIsTrue_whenOnMsg_thenChangeOwnerToExistingCustomer() throws TbNodeException, ExecutionException, InterruptedException, ThingsboardException {
        CustomerId existingCustomerId = new CustomerId(UUID.fromString("60edc2b1-3d37-46b3-9069-963cc98e75e4"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(TENANT_ID);
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("Test Customer");
        config.setCreateOwnerIfNotExists(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.of(new Customer(existingCustomerId))));
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(TENANT_ID, "Test Customer");
        verify(customerServiceMock, never()).saveCustomer(any(Customer.class));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(TENANT_ID, existingCustomerId, DEVICE_ID);
        verifyNoMoreInteractions(ctxMock, customerServiceMock, peContextMock);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 0 with ownerCacheExpiration
                Arguments.of(0,
                        "{\"ownerCacheExpiration\":300,\"createOwnerIfNotExists\":false}",
                        true,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                ),
                // config for version 0
                Arguments.of(0,
                        "{\"createOwnerIfNotExists\":false}",
                        true,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                ),
                // config for version 1
                Arguments.of(1,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}",
                        false,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
