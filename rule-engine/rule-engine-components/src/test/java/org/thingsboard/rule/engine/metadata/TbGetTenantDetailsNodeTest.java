/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.EntityDetails;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbGetTenantDetailsNodeTest {

    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    @Mock
    private TbContext ctxMock;
    @Mock
    private TenantService tenantServiceMock;
    private TbGetTenantDetailsNode node;
    private TbGetTenantDetailsNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private TbMsg msg;
    private Tenant tenant;

    @BeforeEach
    public void setUp() {
        node = new TbGetTenantDetailsNode();
        config = new TbGetTenantDetailsNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        tenant = new Tenant();
        tenant.setId(new TenantId(UUID.randomUUID()));
        tenant.setTitle("Tenant title");
        tenant.setCountry("Tenant country");
        tenant.setCity("Tenant city");
        tenant.setState("Tenant state");
        tenant.setZip("123456");
        tenant.setAddress("Tenant address 1");
        tenant.setAddress2("Tenant address 2");
        tenant.setPhone("+123456789");
        tenant.setEmail("email@tenant.com");
        tenant.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":\"Tenant description\"}"));
    }

    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // GIVEN
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo cannot be null!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenNoEntityDetailsSelected_whenInit_thenException() {
        // GIVEN
        var expectedExceptionMessage = "No entity details selected!";

        config.setDetailsList(Collections.emptyList());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN-WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDetailsList()).isEqualTo(List.of(EntityDetails.STATE, EntityDetails.TITLE));
        assertThat(config.getFetchTo()).isEqualTo(FetchTo.DATA);
        assertThat(node.fetchTo).isEqualTo(FetchTo.DATA);
    }

    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN
        config.setDetailsList(List.of(EntityDetails.ID, EntityDetails.PHONE));
        config.setFetchTo(FetchTo.METADATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDetailsList()).isEqualTo(List.of(EntityDetails.ID, EntityDetails.PHONE));
        assertThat(config.getFetchTo()).isEqualTo(FetchTo.METADATA);
        assertThat(node.fetchTo).isEqualTo(FetchTo.METADATA);
    }

    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // GIVEN
        node.fetchTo = FetchTo.DATA;
        msg = TbMsg.newMsg("SOME_MESSAGE_TYPE", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), "[]");

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenEntityThatDoesNotBelongToTheCurrentTenant_whenOnMsg_thenException() {
        // GIVEN
        var expectedExceptionMessage = "Entity with id: '" + DUMMY_DEVICE_ORIGINATOR +
                "' specified in the configuration doesn't belong to the current tenant.";

        doThrow(new RuntimeException(expectedExceptionMessage)).when(ctxMock).checkTenantEntity(DUMMY_DEVICE_ORIGINATOR);
        msg = TbMsg.newMsg("SOME_MESSAGE_TYPE", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), "{}");

        // WHEN
        var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenAllEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchAllToData() {
        // GIVEN
        prepareMsgAndConfig(FetchTo.DATA, List.of(EntityDetails.values()));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"," +
                "\"tenant_id\":\"" + tenant.getId() + "\"," +
                "\"tenant_title\":\"" + tenant.getTitle() + "\"," +
                "\"tenant_country\":\"" + tenant.getCountry() + "\"," +
                "\"tenant_city\":\"" + tenant.getCity() + "\"," +
                "\"tenant_state\":\"" + tenant.getState() + "\"," +
                "\"tenant_zip\":\"" + tenant.getZip() + "\"," +
                "\"tenant_address\":\"" + tenant.getAddress() + "\"," +
                "\"tenant_address2\":\"" + tenant.getAddress2() + "\"," +
                "\"tenant_phone\":\"" + tenant.getPhone() + "\"," +
                "\"tenant_email\":\"" + tenant.getEmail() + "\"," +
                "\"tenant_additionalInfo\":\"" + tenant.getAdditionalInfo().get("description").asText() + "\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData() {
        // GIVEN
        prepareMsgAndConfig(FetchTo.METADATA, List.of(EntityDetails.ID, EntityDetails.TITLE, EntityDetails.PHONE));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(msg.getMetaData().getData());
        expectedMsgMetaData.putValue("tenant_id", tenant.getId().getId().toString());
        expectedMsgMetaData.putValue("tenant_title", tenant.getTitle());
        expectedMsgMetaData.putValue("tenant_phone", tenant.getPhone());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    @Test
    public void givenNoEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchNothingToMetaData() {
        // GIVEN
        prepareMsgAndConfig(FetchTo.METADATA, Collections.emptyList());

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        tenant.setZip(null);
        tenant.setAddress(null);
        tenant.setAddress2(null);

        prepareMsgAndConfig(FetchTo.DATA, List.of(EntityDetails.ZIP, EntityDetails.ADDRESS, EntityDetails.ADDRESS2));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenDidNotFindTenant_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        prepareMsgAndConfig(FetchTo.DATA, List.of(EntityDetails.ZIP, EntityDetails.ADDRESS, EntityDetails.ADDRESS2));

        when(ctxMock.getTenantId()).thenReturn(tenant.getId());
        when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
        when(tenantServiceMock.findTenantByIdAsync(eq(tenant.getId()), eq(tenant.getId()))).thenReturn(Futures.immediateFuture(null));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    @Test
    public void givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        tenant.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":null}"));

        prepareMsgAndConfig(FetchTo.DATA, List.of(EntityDetails.ADDITIONAL_INFO));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    private void prepareMsgAndConfig(FetchTo fetchTo, List<EntityDetails> detailsList) {
        config.setDetailsList(detailsList);
        config.setFetchTo(fetchTo);

        node.config = config;
        node.fetchTo = fetchTo;

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("metaKey1", "metaValue1");
        msgMetaData.putValue("metaKey2", "metaValue2");

        var msgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"}";

        msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, msgMetaData, msgData);
    }

    private void mockFindTenant() {
        when(ctxMock.getTenantId()).thenReturn(tenant.getId());
        when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
        when(tenantServiceMock.findTenantByIdAsync(eq(tenant.getId()), eq(tenant.getId()))).thenReturn(Futures.immediateFuture(tenant));
    }

}
