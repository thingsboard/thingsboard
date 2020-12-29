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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { Widget, widgetType } from '@shared/models/widget.models';
import { toWidgetInfo } from '@home/models/widget-component.models';
import { DashboardCallbacks } from '../../models/dashboard-component.models';
import { getCurrentAuthState } from "@core/auth/auth.selectors";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit, OnChanges {

  @Input()
  widgetsBundle: WidgetsBundle;

  @Input()
  aliasController: IAliasController;

  @Output()
  widgetSelected: EventEmitter<Widget> = new EventEmitter<Widget>();

  timeseriesWidgetTypes: Array<Widget> = [];
  latestWidgetTypes: Array<Widget> = [];
  rpcWidgetTypes: Array<Widget> = [];
  alarmWidgetTypes: Array<Widget> = [];
  staticWidgetTypes: Array<Widget> = [];

  callbacks: DashboardCallbacks = {
    onWidgetClicked: this.onWidgetClicked.bind(this)
  };

  constructor(private widgetsService: WidgetService,
              private store: Store<AppState>) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue && change.currentValue) {
        if (propName === 'widgetsBundle') {
          this.loadLibrary();
        }
      }
    }
  }

  private loadLibrary() {
    this.timeseriesWidgetTypes.length = 0;
    this.latestWidgetTypes.length = 0;
    this.rpcWidgetTypes.length = 0;
    this.alarmWidgetTypes.length = 0;
    this.staticWidgetTypes.length = 0;
    const bundleAlias = this.widgetsBundle.alias;
    const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
    this.widgetsService.getBundleWidgetTypes(bundleAlias,
      isSystem).subscribe(
      (types) => {
        types = types.sort((a, b) => b.createdTime - a.createdTime);
        let top = 0;
        types.forEach((type) => {
          const widgetTypeInfo = toWidgetInfo(type);
          const widget: Widget = {
            typeId: type.id,
            isSystemType: isSystem,
            bundleAlias,
            typeAlias: widgetTypeInfo.alias,
            type: widgetTypeInfo.type,
            title: widgetTypeInfo.widgetName,
            sizeX: widgetTypeInfo.sizeX,
            sizeY: widgetTypeInfo.sizeY,
            row: top,
            col: 0,
            config: JSON.parse(widgetTypeInfo.defaultConfig)
          };
          widget.config.title = widgetTypeInfo.widgetName;
          switch (widgetTypeInfo.type) {
            case widgetType.timeseries:
              this.timeseriesWidgetTypes.push(widget);
              break;
            case widgetType.latest:
              this.latestWidgetTypes.push(widget);
              break;
            case widgetType.rpc:
              this.rpcWidgetTypes.push(widget);
              break;
            case widgetType.alarm:
              this.alarmWidgetTypes.push(widget);
              break;
            case widgetType.static:
              this.staticWidgetTypes.push(widget);
              break;
          }
          if (!getCurrentAuthState(this.store).edgesSupportEnabled) {
            this.staticWidgetTypes = this.staticWidgetTypes.filter(type => type.typeAlias !== 'edges_instances_overview')
          }
          top += widget.sizeY;
        });
      }
    );
  }

  hasWidgetTypes() {
    return this.timeseriesWidgetTypes.length > 0 ||
           this.latestWidgetTypes.length > 0 ||
           this.rpcWidgetTypes.length > 0 ||
           this.alarmWidgetTypes.length > 0 ||
           this.staticWidgetTypes.length > 0;
  }

  private onWidgetClicked($event: Event, widget: Widget, index: number): void {
    this.widgetSelected.emit(widget);
  }

}
