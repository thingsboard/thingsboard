///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { DatasourceComponent } from '@home/components/widget/config/datasource.component';
import { DatasourcesComponent } from '@home/components/widget/config/datasources.component';
import { WidgetSettingsModule } from '@home/components/widget/lib/settings/widget-settings.module';
import { TimewindowConfigPanelComponent } from '@home/components/widget/config/timewindow-config-panel.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import { TimewindowStyleComponent } from '@home/components/widget/config/timewindow-style.component';
import { TimewindowStylePanelComponent } from '@home/components/widget/config/timewindow-style-panel.component';
import { TargetDeviceComponent } from '@home/components/widget/config/target-device.component';

@NgModule({
  declarations:
    [
      AlarmAssigneeSelectComponent,
      AlarmFilterConfigComponent,
      DatasourceComponent,
      DatasourcesComponent,
      TargetDeviceComponent,
      TimewindowStyleComponent,
      TimewindowStylePanelComponent,
      TimewindowConfigPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetSettingsModule,
    WidgetSettingsCommonModule
  ],
  exports: [
    AlarmAssigneeSelectComponent,
    AlarmFilterConfigComponent,
    DatasourceComponent,
    DatasourcesComponent,
    TargetDeviceComponent,
    TimewindowStyleComponent,
    TimewindowStylePanelComponent,
    TimewindowConfigPanelComponent,
    WidgetSettingsCommonModule
  ]
})
export class WidgetConfigComponentsModule { }
