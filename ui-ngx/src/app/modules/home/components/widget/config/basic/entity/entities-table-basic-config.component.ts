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
  datasourcesHasOnlyComparisonAggregation, WidgetConfig,
  widgetTitleAutocompleteValues
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';

@Component({
    selector: 'tb-entities-table-basic-config',
    templateUrl: './entities-table-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
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

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.entitiesTableWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'name', type: DataKeyType.entityField }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.entitiesTableWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      columns: [this.getColumns(configData.config.datasources), []],
      showTitle: [configData.config.showTitle, []],
      title: [configData.config.settings?.entitiesTitle, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      color: [configData.config.color, []],
      backgroundColor: [configData.config.backgroundColor, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
    this.setColumns(config.columns, this.widgetConfig.config.datasources);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.entitiesTitle = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;
    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.color = config.color;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.entitiesTableWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.entitiesTableWidgetConfigForm.get('showTitleIcon').value;
    if (showTitle) {
      this.entitiesTableWidgetConfigForm.get('title').enable();
      this.entitiesTableWidgetConfigForm.get('titleFont').enable();
      this.entitiesTableWidgetConfigForm.get('titleColor').enable();
      this.entitiesTableWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.entitiesTableWidgetConfigForm.get('titleIcon').enable();
        this.entitiesTableWidgetConfigForm.get('iconColor').enable();
      } else {
        this.entitiesTableWidgetConfigForm.get('titleIcon').disable();
        this.entitiesTableWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.entitiesTableWidgetConfigForm.get('title').disable();
      this.entitiesTableWidgetConfigForm.get('titleFont').disable();
      this.entitiesTableWidgetConfigForm.get('titleColor').disable();
      this.entitiesTableWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.entitiesTableWidgetConfigForm.get('titleIcon').disable();
      this.entitiesTableWidgetConfigForm.get('iconColor').disable();
    }
    this.entitiesTableWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.entitiesTableWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
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

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.settings?.enableSearch) || config.settings?.enableSearch) {
      buttons.push('search');
    }
    if (isUndefined(config.settings?.enableSelectColumnDisplay) || config.settings?.enableSelectColumnDisplay) {
      buttons.push('columnsToDisplay');
    }
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.settings.enableSearch = buttons.includes('search');
    config.settings.enableSelectColumnDisplay = buttons.includes('columnsToDisplay');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
