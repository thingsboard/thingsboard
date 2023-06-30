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
import { EntitiesTableWidgetComponent } from '@home/components/widget/lib/entities-table-widget.component';
import { DisplayColumnsPanelComponent } from '@home/components/widget/lib/display-columns-panel.component';
import { AlarmsTableWidgetComponent } from '@home/components/widget/lib/alarms-table-widget.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { TimeseriesTableWidgetComponent } from '@home/components/widget/lib/timeseries-table-widget.component';
import { EntitiesHierarchyWidgetComponent } from '@home/components/widget/lib/entities-hierarchy-widget.component';
import { RpcWidgetsModule } from '@home/components/widget/lib/rpc/rpc-widgets.module';
import {
  DateRangeNavigatorPanelComponent,
  DateRangeNavigatorWidgetComponent
} from '@home/components/widget/lib/date-range-navigator/date-range-navigator.component';
import { MultipleInputWidgetComponent } from '@home/components/widget/lib/multiple-input-widget.component';
import { TripAnimationComponent } from '@home/components/widget/lib/trip-animation/trip-animation.component';
import { PhotoCameraInputWidgetComponent } from '@home/components/widget/lib/photo-camera-input.component';
import { GatewayFormComponent } from '@home/components/widget/lib/gateway/gateway-form.component';
import { NavigationCardsWidgetComponent } from '@home/components/widget/lib/navigation-cards-widget.component';
import { NavigationCardWidgetComponent } from '@home/components/widget/lib/navigation-card-widget.component';
import { EdgesOverviewWidgetComponent } from '@home/components/widget/lib/edges-overview-widget.component';
import { JsonInputWidgetComponent } from '@home/components/widget/lib/json-input-widget.component';
import { QrCodeWidgetComponent } from '@home/components/widget/lib/qrcode-widget.component';
import { MarkdownWidgetComponent } from '@home/components/widget/lib/markdown-widget.component';
import { SelectEntityDialogComponent } from '@home/components/widget/lib/maps/dialogs/select-entity-dialog.component';
import { HomePageWidgetsModule } from '@home/components/widget/lib/home-page/home-page-widgets.module';
import { WIDGET_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { FlotWidgetComponent } from '@home/components/widget/lib/flot-widget.component';
import { LegendComponent } from '@home/components/widget/lib/legend.component';
import { GatewayConnectorComponent } from '@home/components/widget/lib/device/gateway-connectors.component';
import { GatewayLogsComponent } from '@home/components/widget/lib/device/gateway-logs.component';
import { GatewayStatisticsComponent } from '@home/components/widget/lib/device/gateway-statistics.component';
import { GatewayServiceRPCComponent } from '@home/components/widget/lib/device/gateway-service-rpc.component';
import { DeviceGatewayCommandComponent } from '@home/components/widget/lib/device/device-gateway-command.component';
import { GatewayConfigurationComponent } from '@home/components/widget/lib/device/gateway-configuration.component';

@NgModule({
  declarations:
    [
      DisplayColumnsPanelComponent,
      EntitiesTableWidgetComponent,
      AlarmsTableWidgetComponent,
      TimeseriesTableWidgetComponent,
      EntitiesHierarchyWidgetComponent,
      EdgesOverviewWidgetComponent,
      DateRangeNavigatorWidgetComponent,
      DateRangeNavigatorPanelComponent,
      JsonInputWidgetComponent,
      MultipleInputWidgetComponent,
      TripAnimationComponent,
      PhotoCameraInputWidgetComponent,
      GatewayFormComponent,
      NavigationCardsWidgetComponent,
      NavigationCardWidgetComponent,
      QrCodeWidgetComponent,
      MarkdownWidgetComponent,
      SelectEntityDialogComponent,
      LegendComponent,
      FlotWidgetComponent,
      GatewayConnectorComponent,
      GatewayLogsComponent,
      GatewayStatisticsComponent,
      GatewayServiceRPCComponent,
      DeviceGatewayCommandComponent,
      GatewayConfigurationComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    RpcWidgetsModule,
    HomePageWidgetsModule,
    SharedHomeComponentsModule
  ],
    exports: [
        EntitiesTableWidgetComponent,
        AlarmsTableWidgetComponent,
        TimeseriesTableWidgetComponent,
        EntitiesHierarchyWidgetComponent,
        EdgesOverviewWidgetComponent,
        RpcWidgetsModule,
        HomePageWidgetsModule,
        DateRangeNavigatorWidgetComponent,
        JsonInputWidgetComponent,
        MultipleInputWidgetComponent,
        TripAnimationComponent,
        PhotoCameraInputWidgetComponent,
        GatewayFormComponent,
        NavigationCardsWidgetComponent,
        NavigationCardWidgetComponent,
        QrCodeWidgetComponent,
        MarkdownWidgetComponent,
        LegendComponent,
        FlotWidgetComponent,
        GatewayConnectorComponent,
        GatewayLogsComponent,
        GatewayStatisticsComponent,
        GatewayServiceRPCComponent,
        DeviceGatewayCommandComponent,
        GatewayConfigurationComponent
    ],
  providers: [
    {provide: WIDGET_COMPONENTS_MODULE_TOKEN, useValue: WidgetComponentsModule }
  ]
})
export class WidgetComponentsModule {
}
