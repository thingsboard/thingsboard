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
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';

@Component({
  selector: 'tb-flot-basic-config',
  templateUrl: './flot-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class FlotBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.flotWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  flotWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.flotWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries, units: '°C', decimals: 0 }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.flotWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      series: [this.getSeries(configData.config.datasources), []],
      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      color: [configData.config.color, []],
      backgroundColor: [configData.config.backgroundColor, []],
      verticalLines: [configData.config.settings?.grid?.verticalLines, []],
      horizontalLines: [configData.config.settings?.grid?.horizontalLines, []],
      showLegend: [configData.config.settings?.showLegend, []],
      legendConfig: [configData.config.settings?.legendConfig, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
    this.setSeries(config.series, this.widgetConfig.config.datasources);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;
    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.color = config.color;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    this.widgetConfig.config.settings.grid = this.widgetConfig.config.settings.grid || {};
    this.widgetConfig.config.settings.grid.verticalLines = config.verticalLines;
    this.widgetConfig.config.settings.grid.horizontalLines = config.horizontalLines;
    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendConfig = config.legendConfig;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon', 'showLegend'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.flotWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.flotWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.flotWidgetConfigForm.get('showLegend').value;
    if (showTitle) {
      this.flotWidgetConfigForm.get('title').enable();
      this.flotWidgetConfigForm.get('titleFont').enable();
      this.flotWidgetConfigForm.get('titleColor').enable();
      this.flotWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.flotWidgetConfigForm.get('titleIcon').enable();
        this.flotWidgetConfigForm.get('iconColor').enable();
      } else {
        this.flotWidgetConfigForm.get('titleIcon').disable();
        this.flotWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.flotWidgetConfigForm.get('title').disable();
      this.flotWidgetConfigForm.get('titleFont').disable();
      this.flotWidgetConfigForm.get('titleColor').disable();
      this.flotWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.flotWidgetConfigForm.get('titleIcon').disable();
      this.flotWidgetConfigForm.get('iconColor').disable();
    }
    if (showLegend) {
      this.flotWidgetConfigForm.get('legendConfig').enable();
    } else {
      this.flotWidgetConfigForm.get('legendConfig').disable();
    }
    this.flotWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.flotWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.flotWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.flotWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.flotWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.flotWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
    this.flotWidgetConfigForm.get('legendConfig').updateValueAndValidity({emitEvent});
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = series;
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
