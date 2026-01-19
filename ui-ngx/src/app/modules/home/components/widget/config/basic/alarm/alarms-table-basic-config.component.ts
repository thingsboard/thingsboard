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
import { DataKey, Datasource, WidgetConfig } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';

@Component({
  selector: 'tb-alarms-table-basic-config',
  templateUrl: './alarms-table-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class AlarmsTableBasicConfigComponent extends BasicWidgetConfigComponent {

  public get alarmSource(): Datasource {
    const datasources: Datasource[] = this.alarmsTableWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  alarmsTableWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmsTableWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.alarmsTableWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      alarmFilterConfig: [configData.config.alarmFilterConfig, []],
      datasources: [[configData.config.alarmSource], []],
      columns: [this.getColumns(configData.config.alarmSource), []],
      showTitle: [configData.config.showTitle, []],
      title: [configData.config.settings?.alarmsTitle, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      displayActivity: [configData.config.settings?.displayActivity, []],
      displayDetails: [configData.config.settings?.displayDetails, []],
      allowAssign: [configData.config.settings?.allowAssign, []],
      allowAcknowledgment: [configData.config.settings?.allowAcknowledgment, []],
      allowClear: [configData.config.settings?.allowClear, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      color: [configData.config.color, []],
      backgroundColor: [configData.config.backgroundColor, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.alarmFilterConfig = config.alarmFilterConfig;
    this.widgetConfig.config.alarmSource = config.datasources[0];
    this.setColumns(config.columns, this.widgetConfig.config.alarmSource);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.alarmsTitle = config.title;
    this.widgetConfig.config.settings.displayActivity = config.displayActivity;
    this.widgetConfig.config.settings.displayDetails = config.displayDetails;
    this.widgetConfig.config.settings.allowAssign = config.allowAssign;
    this.widgetConfig.config.settings.allowAcknowledgment = config.allowAcknowledgment;
    this.widgetConfig.config.settings.allowClear = config.allowClear;

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
    const showTitle: boolean = this.alarmsTableWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.alarmsTableWidgetConfigForm.get('showTitleIcon').value;
    if (showTitle) {
      this.alarmsTableWidgetConfigForm.get('title').enable();
      this.alarmsTableWidgetConfigForm.get('titleFont').enable();
      this.alarmsTableWidgetConfigForm.get('titleColor').enable();
      this.alarmsTableWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.alarmsTableWidgetConfigForm.get('titleIcon').enable();
        this.alarmsTableWidgetConfigForm.get('iconColor').enable();
      } else {
        this.alarmsTableWidgetConfigForm.get('titleIcon').disable();
        this.alarmsTableWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.alarmsTableWidgetConfigForm.get('title').disable();
      this.alarmsTableWidgetConfigForm.get('titleFont').disable();
      this.alarmsTableWidgetConfigForm.get('titleColor').disable();
      this.alarmsTableWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.alarmsTableWidgetConfigForm.get('titleIcon').disable();
      this.alarmsTableWidgetConfigForm.get('iconColor').disable();
    }
    this.alarmsTableWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.alarmsTableWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
  }

  private getColumns(alarmSource?: Datasource): DataKey[] {
    if (alarmSource) {
      return alarmSource.dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], alarmSource?: Datasource) {
    if (alarmSource) {
      alarmSource.dataKeys = columns;
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.settings?.enableSearch) || config.settings?.enableSearch) {
      buttons.push('search');
    }
    if (isUndefined(config.settings?.enableFilter) || config.settings?.enableFilter) {
      buttons.push('filter');
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
    config.settings.enableFilter = buttons.includes('filter');
    config.settings.enableSelectColumnDisplay = buttons.includes('columnsToDisplay');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
