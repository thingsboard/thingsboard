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
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V4_3_1_3MigrationTest {

    @Mock
    private WidgetsBundleService widgetsBundleService;

    @Mock
    private WidgetTypeService widgetTypeService;

    @InjectMocks
    private V4_3_1_3Migration migration;

    @Test
    void versionIs4313() {
        assertEquals("4.3.1.3", migration.getVersion());
    }

    @Test
    void deprecatesTypesThenDeletesBundleEntityOnly() {
        WidgetsBundle bundle = new WidgetsBundle();
        bundle.setId(new WidgetsBundleId(UUID.randomUUID()));
        bundle.setAlias("air_quality");

        WidgetTypeDetails fresh = new WidgetTypeDetails();
        fresh.setId(new WidgetTypeId(UUID.randomUUID()));
        fresh.setDeprecated(false);
        WidgetTypeDetails already = new WidgetTypeDetails();
        already.setId(new WidgetTypeId(UUID.randomUUID()));
        already.setDeprecated(true);

        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "air_quality")).thenReturn(bundle);
        when(widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(TenantId.SYS_TENANT_ID, bundle.getId())).thenReturn(List.of(fresh, already));
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "indoor_environment")).thenReturn(null);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "industrial_widgets")).thenReturn(null);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "outdoor_environment")).thenReturn(null);

        migration.apply();

        // only the previously non-deprecated type is re-saved, now deprecated
        verify(widgetTypeService).saveWidgetType(argThat(WidgetTypeDetails::isDeprecated));
        // widget types are NOT deleted
        verify(widgetTypeService, never()).deleteWidgetTypesByBundleId(any(), any());
        // only the bundle entity is removed
        verify(widgetsBundleService).deleteWidgetsBundle(TenantId.SYS_TENANT_ID, bundle.getId());
    }

    @Test
    void absentBundleIsNoOp() {
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(any(), any())).thenReturn(null);

        migration.apply();

        verify(widgetTypeService, never()).saveWidgetType(any());
        verify(widgetsBundleService, never()).deleteWidgetsBundle(any(), any());
    }
}
