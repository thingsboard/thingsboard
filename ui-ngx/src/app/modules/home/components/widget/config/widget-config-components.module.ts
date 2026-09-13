// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
