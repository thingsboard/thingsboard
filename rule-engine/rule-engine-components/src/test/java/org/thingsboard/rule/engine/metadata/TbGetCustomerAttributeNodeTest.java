// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.metadata;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbGetCustomerAttributeNodeTest extends AbstractRuleNodeUpgradeTest {

    private final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private final ListeningExecutor DB_EXECUTOR = DirectListeningExecutor.INSTANCE;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AttributesService attributesServiceMock;
    @Mock
    private TimeseriesService timeseriesServiceMock;
    @Mock
    private EntityService entityServiceMock;

    private TbGetCustomerAttributeNode node;
    private TbGetCustomerAttributeNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private TbMsg msg;

    @BeforeEach
    public void setup() {
        node = spy(new TbGetCustomerAttributeNode());
        config = new TbGetCustomerAttributeNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        lenient().when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        lenient().when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        lenient().when(ctxMock.getEntityService()).thenReturn(entityServiceMock);
    }

    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // GIVEN
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenConfigWithNullDataToFetch_whenInit_thenException() {
        // GIVEN
        config.setDataToFetch(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("DataToFetch property has invalid value: null. Only ATTRIBUTES and LATEST_TELEMETRY values supported!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenConfigWithUnsupportedDataToFetch_whenInit_thenException() {
        // GIVEN
        config.setDataToFetch(DataToFetch.FIELDS);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("DataToFetch property has invalid value: FIELDS. Only ATTRIBUTES and LATEST_TELEMETRY values supported!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of("alarmThreshold", "threshold"));
        assertThat(config.getDataToFetch()).isEqualTo(DataToFetch.ATTRIBUTES);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
    }

    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN
        config.setDataMapping(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"));
        config.setDataToFetch(DataToFetch.LATEST_TELEMETRY);
        config.setFetchTo(TbMsgSource.DATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"));
        assertThat(config.getDataToFetch()).isEqualTo(DataToFetch.LATEST_TELEMETRY);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.DATA);
    }

    @Test
    public void givenEmptyAttributesMapping_whenInit_thenException() {
        // GIVEN
        var expectedExceptionMessage = "At least one mapping entry should be specified!";

        config.setDataMapping(Collections.emptyMap());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // GIVEN
        node.fetchTo = TbMsgSource.DATA;
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenDidNotFindEntity_whenOnMsg_thenShouldTellFailure() throws TbNodeException {
        // GIVEN
        config.setPreserveOriginatorIfCustomer(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var userId = new UserId(UUID.randomUUID());

        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(userId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, userId)).thenReturn(
                FluentFuture.from(immediateFuture(Optional.empty()))
        );

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock).tellFailure(actualMessageCaptor.capture(), actualExceptionCaptor.capture());

        var actualMessage = actualMessageCaptor.getValue();
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMessage);
        assertEquals("Originator not found", actualException.getMessage());
        assertInstanceOf(NoSuchElementException.class, actualException);
    }

    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldFetchAttributesToData() throws TbNodeException {
        // GIVEN
        var device = new Device(new DeviceId(UUID.randomUUID()));
        device.setCustomerId(CUSTOMER_ID);

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.ATTRIBUTES, device.getId(), true);

        List<AttributeKvEntry> attributesList = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, device.getId())).thenReturn(
                FluentFuture.from(immediateFuture(Optional.of(CUSTOMER_ID)))
        );

        when(attributesServiceMock.find(eq(TENANT_ID), eq(CUSTOMER_ID), eq(AttributeScope.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(immediateFuture(attributesList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenFetchAttributesToMetaData_whenOnMsg_thenShouldFetchAttributesToMetaData() throws TbNodeException {
        // GIVEN
        var user = new User(new UserId(UUID.randomUUID()));
        user.setCustomerId(CUSTOMER_ID);

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.ATTRIBUTES, user.getId(), true);

        List<AttributeKvEntry> attributesList = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, user.getId())).thenReturn(
                FluentFuture.from(immediateFuture(Optional.of(CUSTOMER_ID)))
        );

        when(attributesServiceMock.find(eq(TENANT_ID), eq(CUSTOMER_ID), eq(AttributeScope.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(immediateFuture(attributesList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    @Test
    public void givenFetchTelemetryToData_whenOnMsg_thenShouldFetchTelemetryToData() throws TbNodeException {
        // GIVEN
        var customer = new Customer(new CustomerId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.LATEST_TELEMETRY, customer.getId(), true);

        List<TsKvEntry> timeseriesList = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(timeseriesServiceMock.findLatest(eq(TENANT_ID), eq(customer.getId()), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(immediateFuture(timeseriesList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verifyNoInteractions(entityServiceMock);

        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenFetchTelemetryToMetaData_whenOnMsg_thenShouldFetchTelemetryToMetaData() throws TbNodeException {
        // GIVEN
        var asset = new Asset(new AssetId(UUID.randomUUID()));
        asset.setCustomerId(CUSTOMER_ID);

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.LATEST_TELEMETRY, asset.getId(), true);

        List<TsKvEntry> timeseriesList = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, asset.getId())).thenReturn(
                FluentFuture.from(immediateFuture(Optional.of(CUSTOMER_ID)))
        );

        when(timeseriesServiceMock.findLatest(eq(TENANT_ID), eq(CUSTOMER_ID), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(immediateFuture(timeseriesList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    @Test
    void givenPreserveOriginatorIfCustomerIsFalse_whenOnMsg_thenShouldFetchParentsAttributesToData() throws TbNodeException {
        // GIVEN
        UUID parentCustomerId = Uuids.timeBased();
        UUID customerId = Uuids.timeBased();
        var customer = new Customer(new CustomerId(customerId));
        customer.setParentCustomerId(new CustomerId(parentCustomerId));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.LATEST_TELEMETRY, customer.getId(), false);

        List<TsKvEntry> timeseriesList = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, customer.getId())).thenReturn(
                FluentFuture.from(immediateFuture(Optional.of(customer.getParentCustomerId())))
        );

        when(timeseriesServiceMock.findLatest(any(), any(), anyList()))
                .thenReturn(immediateFuture(timeseriesList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(timeseriesServiceMock).findLatest(eq(TENANT_ID), eq(customer.getParentCustomerId()), argThat(new ListMatcher<>(expectedPatternProcessedKeysList)));
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    void givenPreserveOriginatorIfCustomerIsFalse_whenOnMsg_thenShouldThrowException() throws TbNodeException {
        // GIVEN
        UUID customerId = Uuids.timeBased();
        var customer = new Customer(new CustomerId(customerId));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.LATEST_TELEMETRY, customer.getId(), false);

        when(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, customer.getId())).thenReturn(
                FluentFuture.from(immediateFuture(Optional.of(new CustomerId(EntityId.NULL_UUID))))
        );

        // WHEN
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        node.onMsg(ctxMock, msg);

        //THEN
        verify(ctxMock).tellFailure(eq(msg), exceptionCaptor.capture());

        Exception capturedException = exceptionCaptor.getValue();

        assertInstanceOf(IllegalStateException.class, capturedException);
        assertEquals("Originator is not assigned to any customer", exceptionCaptor.getValue().getMessage());
    }

    private void prepareMsgAndConfig(TbMsgSource fetchTo, DataToFetch dataToFetch, EntityId originator, boolean preserveOriginatorIfCustomer) throws TbNodeException {
        config.setDataMapping(Map.of(
                "sourceKey1", "targetKey1",
                "${metaDataPattern1}", "$[messageBodyPattern1]",
                "$[messageBodyPattern2]", "${metaDataPattern2}"));
        config.setDataToFetch(dataToFetch);
        config.setFetchTo(fetchTo);
        config.setPreserveOriginatorIfCustomer(preserveOriginatorIfCustomer);

        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("metaDataPattern1", "sourceKey2");
        msgMetaData.putValue("metaDataPattern2", "targetKey3");

        var msgData = "{\"temp\":42,\"humidity\":77,\"messageBodyPattern1\":\"targetKey2\",\"messageBodyPattern2\":\"sourceKey3\"}";

        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();
    }

    @RequiredArgsConstructor
    private static class ListMatcher<T> implements ArgumentMatcher<List<T>> {

        private final List<T> expectedList;

        @Override
        public boolean matches(List<T> actualList) {
            if (actualList == expectedList) {
                return true;
            }
            if (actualList.size() != expectedList.size()) {
                return false;
            }
            return actualList.containsAll(expectedList);
        }

    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\"attrMapping\":{\"alarmThreshold\":\"threshold\"},\"telemetry\":false}",
                        true,
                        "{\"dataToFetch\": \"ATTRIBUTES\", \"dataMapping\": {\"alarmThreshold\":\"threshold\"}, \"fetchTo\": \"METADATA\", \"preserveOriginatorIfCustomer\": true}"),
                //config for version 1
                Arguments.of(1,
                        "{\"dataToFetch\": \"ATTRIBUTES\", \"dataMapping\": {\"alarmThreshold\":\"threshold\"}, \"fetchTo\": \"METADATA\"}",
                        true,
                        "{\"dataToFetch\": \"ATTRIBUTES\", \"dataMapping\": {\"alarmThreshold\":\"threshold\"}, \"fetchTo\": \"METADATA\", \"preserveOriginatorIfCustomer\": true}"),
                //config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"dataToFetch\": \"ATTRIBUTES\", \"dataMapping\": { \"temp\": \"temp\"}, \"fetchTo\": \"METADATA\", \"preserveOriginatorIfCustomer\": true}",
                        false,
                        "{\"dataToFetch\": \"ATTRIBUTES\", \"dataMapping\": { \"temp\": \"temp\"}, \"fetchTo\": \"METADATA\", \"preserveOriginatorIfCustomer\": true}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
