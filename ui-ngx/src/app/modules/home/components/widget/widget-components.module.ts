///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { AlarmFilterPanelComponent } from '@home/components/widget/lib/alarm-filter-panel.component';
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
import { PhotoCameraInputWidgetComponent } from './lib/photo-camera-input.component';
import { GatewayFormComponent } from './lib/gateway/gateway-form.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { NavigationCardsWidgetComponent } from '@home/components/widget/lib/navigation-cards-widget.component';
import { NavigationCardWidgetComponent } from '@home/components/widget/lib/navigation-card-widget.component';
import { EdgesOverviewWidgetComponent } from '@home/components/widget/lib/edges-overview-widget.component';

@NgModule({
  declarations:
    [
      DisplayColumnsPanelComponent,
      AlarmFilterPanelComponent,
      EntitiesTableWidgetComponent,
      AlarmsTableWidgetComponent,
      TimeseriesTableWidgetComponent,
      EntitiesHierarchyWidgetComponent,
      EdgesOverviewWidgetComponent,
      DateRangeNavigatorWidgetComponent,
      DateRangeNavigatorPanelComponent,
      MultipleInputWidgetComponent,
      TripAnimationComponent,
      PhotoCameraInputWidgetComponent,
      GatewayFormComponent,
      NavigationCardsWidgetComponent,
      NavigationCardWidgetComponent
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
    EdgesOverviewWidgetComponent,
    RpcWidgetsModule,
    DateRangeNavigatorWidgetComponent,
    MultipleInputWidgetComponent,
    TripAnimationComponent,
    PhotoCameraInputWidgetComponent,
    GatewayFormComponent,
    NavigationCardsWidgetComponent,
    NavigationCardWidgetComponent
  ],
  providers: [
    CustomDialogService,
    ImportExportService
  ]
})
export class WidgetComponentsModule { }
