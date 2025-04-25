/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.util;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.user.UserService;

import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EntitiesCustomerIdAsyncLoaderTest {

    ListeningExecutor dbExecutor = new TestDbCallbackExecutor();

    @Mock
    TbContext ctxMock;
    @Mock
    UserService userServiceMock;
    @Mock
    AssetService assetServiceMock;
    @Mock
    DeviceService deviceServiceMock;
    @Mock
    EntityViewService entityViewServiceMock;
    @Mock
    EdgeService edgeServiceMock;

    TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    CustomerId customerId = new CustomerId(Uuids.timeBased());

    @Test
    void givenCustomerOriginator_thenReturnsOriginatorItself() throws ExecutionException, InterruptedException {
        // GIVEN-WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, customerId).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @Test
    void testUserOriginator() throws ExecutionException, InterruptedException {
        // GIVEN
        var user = new User(new UserId(Uuids.timeBased()));
        user.setCustomerId(customerId);

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getUserService()).willReturn(userServiceMock);
        given(userServiceMock.findUserByIdAsync(tenantId, user.getId())).willReturn(immediateFuture(user));
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, user.getId()).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @Test
    void testAssetOriginator() throws ExecutionException, InterruptedException {
        // GIVEN
        var asset = new Asset(new AssetId(Uuids.timeBased()));
        asset.setCustomerId(customerId);

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAssetService()).willReturn(assetServiceMock);
        given(assetServiceMock.findAssetByIdAsync(tenantId, asset.getId())).willReturn(immediateFuture(asset));
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, asset.getId()).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @Test
    void testDeviceOriginator() throws ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device(new DeviceId(Uuids.timeBased()));
        device.setCustomerId(customerId);

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getDeviceService()).willReturn(deviceServiceMock);
        given(deviceServiceMock.findDeviceById(tenantId, device.getId())).willReturn(device);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, device.getId()).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @Test
    void testEntityViewOriginator() throws ExecutionException, InterruptedException {
        // GIVEN
        var entityView = new EntityView(new EntityViewId(Uuids.timeBased()));
        entityView.setCustomerId(customerId);

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getEntityViewService()).willReturn(entityViewServiceMock);
        given(entityViewServiceMock.findEntityViewByIdAsync(tenantId, entityView.getId())).willReturn(immediateFuture(entityView));
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, entityView.getId()).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @Test
    void testEdgeOriginator() throws ExecutionException, InterruptedException {
        // GIVEN
        var edge = new Edge(new EdgeId(Uuids.timeBased()));
        edge.setCustomerId(customerId);

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getEdgeService()).willReturn(edgeServiceMock);
        given(edgeServiceMock.findEdgeByIdAsync(tenantId, edge.getId())).willReturn(immediateFuture(edge));
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, edge.getId()).get();

        // THEN
        assertThat(actualCustomerId).isEqualTo(customerId);
    }

    @ParameterizedTest
    @EnumSource(
            value = EntityType.class,
            names = {"CUSTOMER", "USER", "DEVICE", "ASSET", "ENTITY_VIEW", "EDGE"}, // supported entity types
            mode = EnumSource.Mode.EXCLUDE
    )
    void givenEntityTypeThatHasNoCustomer_thenThrowsException(EntityType entityType) {
        // GIVEN
        var entityId = EntityIdFactory.getByTypeAndUuid(entityType, Uuids.timeBased());

        // WHEN-THEN
        assertThatThrownBy(() -> EntitiesCustomerIdAsyncLoader.findEntityCustomerIdAsync(ctxMock, entityId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TbNodeException.class)
                .hasMessage("org.thingsboard.rule.engine.api.TbNodeException: Unexpected originator EntityType: " + entityType);
    }

}
