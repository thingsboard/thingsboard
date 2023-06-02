///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AlarmFilterConfigComponent } from '@home/components/alarm/alarm-filter-config.component';
import { AlarmAssigneeSelectComponent } from '@home/components/alarm/alarm-assignee-select.component';
import { DataKeysComponent } from '@home/components/widget/config/data-keys.component';
import { DataKeyConfigDialogComponent } from '@home/components/widget/config/data-key-config-dialog.component';
import { DataKeyConfigComponent } from '@home/components/widget/config/data-key-config.component';
import { DatasourceComponent } from '@home/components/widget/config/datasource.component';
import { DatasourcesComponent } from '@home/components/widget/config/datasources.component';
import { EntityAliasSelectComponent } from '@home/components/alias/entity-alias-select.component';
import { FilterSelectComponent } from '@home/components/filter/filter-select.component';
import { WidgetSettingsModule } from '@home/components/widget/lib/settings/widget-settings.module';
import { WidgetSettingsComponent } from '@home/components/widget/config/widget-settings.component';
import { TimewindowConfigPanelComponent } from '@home/components/widget/config/timewindow-config-panel.component';
import { WidgetUnitsComponent } from '@home/components/widget/config/widget-units.component';

@NgModule({
  declarations:
    [
      AlarmAssigneeSelectComponent,
      AlarmFilterConfigComponent,
      DataKeysComponent,
      DataKeyConfigDialogComponent,
      DataKeyConfigComponent,
      DatasourceComponent,
      DatasourcesComponent,
      EntityAliasSelectComponent,
      FilterSelectComponent,
      TimewindowConfigPanelComponent,
      WidgetUnitsComponent,
      WidgetSettingsComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetSettingsModule
  ],
  exports: [
    AlarmAssigneeSelectComponent,
    AlarmFilterConfigComponent,
    DataKeysComponent,
    DataKeyConfigDialogComponent,
    DataKeyConfigComponent,
    DatasourceComponent,
    DatasourcesComponent,
    EntityAliasSelectComponent,
    FilterSelectComponent,
    TimewindowConfigPanelComponent,
    WidgetUnitsComponent,
    WidgetSettingsComponent
  ]
})
export class WidgetConfigComponentsModule { }
