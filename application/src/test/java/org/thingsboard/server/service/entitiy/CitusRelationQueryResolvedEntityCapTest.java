// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.CitusDaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the Citus-only defensive cap on coordinator-side relation resolution
 * ({@code database.citus.relation_query.max_resolved_entities}): a relations query that resolves more
 * entities than the configured limit must fail fast with a client-mappable {@code IncorrectParameterException}
 * whose message names the property, instead of materializing an unbounded set on the coordinator heap.
 */
@CitusDaoSqlTest
@TestPropertySource(properties = "database.citus.relation_query.max_resolved_entities=2")
public class CitusRelationQueryResolvedEntityCapTest extends AbstractControllerTest {

    @Autowired
    EntityService entityService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    RelationService relationService;

    @Test
    public void testRelationsQueryFailsFastWhenResolvedEntityCapIsExceeded() {
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Cap breach device " + i);
            device.setType("default");
            Device savedDevice = deviceService.saveDevice(device);
            relationService.saveRelation(tenantId, new EntityRelation(tenantId, savedDevice.getId(), "Contains", RelationTypeGroup.COMMON));
        }

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);

        MergedUserPermissions permissions = new MergedUserPermissions(
                Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap());
        EntityDataQuery query = new EntityDataQuery(filter,
                new EntityDataPageLink(10, 0, null, null),
                List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name")), null, null);

        assertThatThrownBy(() -> entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), permissions, query))
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("database.citus.relation_query.max_resolved_entities");
    }

}
