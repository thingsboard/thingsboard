///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { EntitiesTableWidgetComponent } from '@home/components/widget/lib/entities-table-widget.component';
import { DisplayColumnsPanelComponent } from '@home/components/widget/lib/display-columns-panel.component';
import { AlarmsTableWidgetComponent } from '@home/components/widget/lib/alarms-table-widget.component';
import { AlarmStatusFilterPanelComponent } from '@home/components/widget/lib/alarm-status-filter-panel.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { TimeseriesTableWidgetComponent } from '@home/components/widget/lib/timeseries-table-widget.component';
import { EntitiesHierarchyWidgetComponent } from '@home/components/widget/lib/entities-hierarchy-widget.component';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { RpcWidgetsModule } from '@home/components/widget/lib/rpc/rpc-widgets.module';
import {
  DateRangeNavigatorPanelComponent,
  DateRangeNavigatorWidgetComponent
} from '@home/components/widget/lib/date-range-navigator/date-range-navigator.component';
import { MultipleInputWidgetComponent } from './lib/multiple-input-widget.component';
import { TripAnimationComponent } from './trip-animation/trip-animation.component';
import { WebCameraInputWidgetComponent } from './lib/web-camera-input.component';
import { GatewayFormComponent } from './lib/gateway/gateway-form.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';

@NgModule({
  declarations:
    [
      DisplayColumnsPanelComponent,
      AlarmStatusFilterPanelComponent,
      EntitiesTableWidgetComponent,
      AlarmsTableWidgetComponent,
      TimeseriesTableWidgetComponent,
      EntitiesHierarchyWidgetComponent,
      DateRangeNavigatorWidgetComponent,
      DateRangeNavigatorPanelComponent,
      MultipleInputWidgetComponent,
      TripAnimationComponent,
      WebCameraInputWidgetComponent,
      GatewayFormComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    RpcWidgetsModule,
    SharedHomeComponentsModule
  ],
  exports: [
    EntitiesTableWidgetComponent,
    AlarmsTableWidgetComponent,
    TimeseriesTableWidgetComponent,
    EntitiesHierarchyWidgetComponent,
    RpcWidgetsModule,
    DateRangeNavigatorWidgetComponent,
    MultipleInputWidgetComponent,
    TripAnimationComponent,
    WebCameraInputWidgetComponent,
    GatewayFormComponent
  ],
  providers: [
    CustomDialogService,
    ImportExportService
  ]
})
export class WidgetComponentsModule { }
