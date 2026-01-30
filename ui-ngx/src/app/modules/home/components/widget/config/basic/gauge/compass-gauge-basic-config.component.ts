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
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { isUndefined } from '@core/utils';

@Component({
    selector: 'tb-compass-gauge-basic-config',
    templateUrl: './compass-gauge-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class CompassGaugeBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.compassGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.compassGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  compassGaugeWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.compassGaugeWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    super.setupDefaults(configData);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.compassGaugeWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      majorTicks: [configData.config.settings?.majorTicks, []],
      majorTickFont: [configData.config.settings?.majorTickFont, []],
      majorTickColor: [configData.config.settings?.majorTickFont.color, []],
      colorMajorTicks: [configData.config.settings?.colorMajorTicks, []],
      colorMinorTicks: [configData.config.settings?.colorMinorTicks, []],
      colorPlate: [configData.config.settings?.colorPlate, []],
      colorNeedle: [configData.config.settings?.colorNeedle, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      backgroundColor: [configData.config.backgroundColor, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
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
    this.widgetConfig.config.backgroundColor = config.backgroundColor;

    this.widgetConfig.config.settings.majorTicks = config.majorTicks;
    this.widgetConfig.config.settings.majorTickFont = config.majorTickFont;
    this.widgetConfig.config.settings.majorTickFont.color = config.majorTickColor;
    this.widgetConfig.config.settings.colorMajorTicks = config.colorMajorTicks;
    this.widgetConfig.config.settings.colorMinorTicks = config.colorMinorTicks;
    this.widgetConfig.config.settings.colorPlate = config.colorPlate;
    this.widgetConfig.config.settings.colorNeedle = config.colorNeedle;

    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.compassGaugeWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.compassGaugeWidgetConfigForm.get('showTitleIcon').value;
    if (showTitle) {
      this.compassGaugeWidgetConfigForm.get('title').enable();
      this.compassGaugeWidgetConfigForm.get('titleFont').enable();
      this.compassGaugeWidgetConfigForm.get('titleColor').enable();
      this.compassGaugeWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.compassGaugeWidgetConfigForm.get('titleIcon').enable();
        this.compassGaugeWidgetConfigForm.get('iconColor').enable();
      } else {
        this.compassGaugeWidgetConfigForm.get('titleIcon').disable();
        this.compassGaugeWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.compassGaugeWidgetConfigForm.get('title').disable();
      this.compassGaugeWidgetConfigForm.get('titleFont').disable();
      this.compassGaugeWidgetConfigForm.get('titleColor').disable();
      this.compassGaugeWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.compassGaugeWidgetConfigForm.get('titleIcon').disable();
      this.compassGaugeWidgetConfigForm.get('iconColor').disable();
    }
    this.compassGaugeWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.compassGaugeWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.compassGaugeWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.compassGaugeWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.compassGaugeWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.compassGaugeWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
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
