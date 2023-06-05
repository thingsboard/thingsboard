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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation
} from '@shared/models/widget.models';

@Component({
  selector: 'tb-entities-table-basic-config',
  templateUrl: './entities-table-basic-config.component.html',
  styleUrls: ['../basic-config.scss', '../../widget-config.scss']
})
export class EntitiesTableBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.entitiesTableWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.entitiesTableWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.entitiesTableWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  entitiesTableWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected configForm(): UntypedFormGroup {
    return this.entitiesTableWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.entitiesTableWidgetConfigForm = this.fb.group({
      timewindowConfig: [{
        useDashboardTimewindow: configData.config.useDashboardTimewindow,
        displayTimewindow: configData.config.useDashboardTimewindow,
        timewindow: configData.config.timewindow
      }, []],
      datasources: [configData.config.datasources, []],
      columns: [this.getColumns(configData.config.datasources), []],
      color: [configData.config.color, []],
      backgroundColor: [configData.config.backgroundColor, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.useDashboardTimewindow = config.timewindowConfig.useDashboardTimewindow;
    this.widgetConfig.config.displayTimewindow = config.timewindowConfig.displayTimewindow;
    this.widgetConfig.config.timewindow = config.timewindowConfig.timewindow;
    this.widgetConfig.config.datasources = config.datasources;
    this.setColumns(config.columns, this.widgetConfig.config.datasources);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.color = config.color;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    return this.widgetConfig;
  }

  private getColumns(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = columns;
    }
  }

}
