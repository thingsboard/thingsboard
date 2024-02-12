/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.user.UserService;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntitiesCustomerIdAsyncLoaderTest {

    private static final EnumSet<EntityType> SUPPORTED_ENTITY_TYPES = EnumSet.of(
            EntityType.CUSTOMER,
            EntityType.USER,
            EntityType.ASSET,
            EntityType.DEVICE
    );
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    @Mock
    private TbContext ctxMock;
    @Mock
    private UserService userServiceMock;
    @Mock
    private AssetService assetServiceMock;
    @Mock
    private DeviceService deviceServiceMock;

    @Test
    public void givenCustomerEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var customer = new Customer(new CustomerId(UUID.randomUUID()));

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, customer.getId()).get();

        // THEN
        assertEquals(customer.getId(), actualCustomerId);
    }

    @Test
    public void givenUserEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var user = new User(new UserId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        user.setCustomerId(expectedCustomerId);

        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        doReturn(Futures.immediateFuture(user)).when(userServiceMock).findUserByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, user.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenAssetEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var asset = new Asset(new AssetId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        asset.setCustomerId(expectedCustomerId);

        when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        doReturn(Futures.immediateFuture(asset)).when(assetServiceMock).findAssetByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, asset.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenDeviceEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device(new DeviceId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        device.setCustomerId(expectedCustomerId);

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        doReturn(device).when(deviceServiceMock).findDeviceById(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, device.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenUnsupportedEntityTypes_whenFindEntityIdAsync_thenException() {
        for (var entityType : EntityType.values()) {
            if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
                var entityId = EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());

                var expectedExceptionMsg = "org.thingsboard.rule.engine.api.TbNodeException: Unexpected originator EntityType: " + entityType;

                var exception = assertThrows(ExecutionException.class,
                        () -> EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, entityId).get());

                assertInstanceOf(TbNodeException.class, exception.getCause());
                assertEquals(expectedExceptionMsg, exception.getMessage());
            }
        }
    }

}
