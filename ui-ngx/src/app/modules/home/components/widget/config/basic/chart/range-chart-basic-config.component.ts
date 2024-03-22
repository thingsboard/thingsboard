///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Injector } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey, legendPositions, legendPositionTranslationMap, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  rangeChartDefaultSettings,
  RangeChartWidgetSettings
} from '@home/components/widget/lib/chart/range-chart-widget.models';

@Component({
  selector: 'tb-range-chart-basic-config',
  templateUrl: './range-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class RangeChartBasicConfigComponent extends BasicWidgetConfigComponent {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  rangeChartWidgetConfigForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.rangeChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: RangeChartWidgetSettings = {...rangeChartDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.rangeChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      dataZoom: [settings.dataZoom, []],
      rangeColors: [settings.rangeColors, []],
      outOfRangeColor: [settings.outOfRangeColor, []],
      fillArea: [settings.fillArea, []],
      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],

      showTooltip: [settings.showTooltip, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipDateInterval: [settings.tooltipDateInterval, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.dataZoom = config.dataZoom;
    this.widgetConfig.config.settings.rangeColors = config.rangeColors;
    this.widgetConfig.config.settings.outOfRangeColor = config.outOfRangeColor;
    this.widgetConfig.config.settings.fillArea = config.fillArea;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendPosition = config.legendPosition;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;
    this.widgetConfig.config.settings.tooltipShowDate = config.tooltipShowDate;
    this.widgetConfig.config.settings.tooltipDateFormat = config.tooltipDateFormat;
    this.widgetConfig.config.settings.tooltipDateFont = config.tooltipDateFont;
    this.widgetConfig.config.settings.tooltipDateColor = config.tooltipDateColor;
    this.widgetConfig.config.settings.tooltipDateInterval = config.tooltipDateInterval;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.rangeChartWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.rangeChartWidgetConfigForm.get('showIcon').value;
    const showLegend: boolean = this.rangeChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.rangeChartWidgetConfigForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.rangeChartWidgetConfigForm.get('tooltipShowDate').value;

    if (showTitle) {
      this.rangeChartWidgetConfigForm.get('title').enable();
      this.rangeChartWidgetConfigForm.get('titleFont').enable();
      this.rangeChartWidgetConfigForm.get('titleColor').enable();
      this.rangeChartWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.rangeChartWidgetConfigForm.get('iconSize').enable();
        this.rangeChartWidgetConfigForm.get('iconSizeUnit').enable();
        this.rangeChartWidgetConfigForm.get('icon').enable();
        this.rangeChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.rangeChartWidgetConfigForm.get('iconSize').disable();
        this.rangeChartWidgetConfigForm.get('iconSizeUnit').disable();
        this.rangeChartWidgetConfigForm.get('icon').disable();
        this.rangeChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.rangeChartWidgetConfigForm.get('title').disable();
      this.rangeChartWidgetConfigForm.get('titleFont').disable();
      this.rangeChartWidgetConfigForm.get('titleColor').disable();
      this.rangeChartWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('iconSize').disable();
      this.rangeChartWidgetConfigForm.get('iconSizeUnit').disable();
      this.rangeChartWidgetConfigForm.get('icon').disable();
      this.rangeChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showLegend) {
      this.rangeChartWidgetConfigForm.get('legendPosition').enable();
      this.rangeChartWidgetConfigForm.get('legendLabelFont').enable();
      this.rangeChartWidgetConfigForm.get('legendLabelColor').enable();
    } else {
      this.rangeChartWidgetConfigForm.get('legendPosition').disable();
      this.rangeChartWidgetConfigForm.get('legendLabelFont').disable();
      this.rangeChartWidgetConfigForm.get('legendLabelColor').disable();
    }

    if (showTooltip) {
      this.rangeChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.rangeChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.rangeChartWidgetConfigForm.get('tooltipShowDate').enable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.rangeChartWidgetConfigForm.get('tooltipDateFormat').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateFont').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateColor').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateInterval').enable();
      } else {
        this.rangeChartWidgetConfigForm.get('tooltipDateFormat').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateFont').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateColor').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.rangeChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.rangeChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipShowDate').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('tooltipDateFormat').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateFont').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateInterval').disable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
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

  private _tooltipValuePreviewFn(): string {
    const units: string = this.rangeChartWidgetConfigForm.get('units').value;
    const decimals: number = this.rangeChartWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.rangeChartWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
