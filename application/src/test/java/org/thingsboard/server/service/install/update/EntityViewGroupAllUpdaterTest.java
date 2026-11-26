// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EntityViewGroupAllUpdaterTest {

    @Test
    void testUnassignFromCustomerSavesWithoutValidation() {
        EntityViewService entityViewService = mock(EntityViewService.class);
        EntityViewGroupAllUpdater updater = new EntityViewGroupAllUpdater(entityViewService, mock(CustomerService.class),
                mock(EntityGroupService.class), new EntityGroup(), true);
        EntityView entityView = new EntityView();
        entityView.setCustomerId(new CustomerId(UUID.randomUUID()));

        updater.unassignFromCustomer(entityView);

        verify(entityViewService).saveEntityView(entityView, false);
        assertThat(entityView.getCustomerId().isNullUid()).isTrue();
    }

}
