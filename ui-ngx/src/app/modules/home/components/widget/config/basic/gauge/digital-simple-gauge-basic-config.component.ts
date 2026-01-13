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

import { AbstractControl, UntypedFormBuilder, UntypedFormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isDefined, isUndefined } from '@core/utils';
import { Component } from '@angular/core';
import {
  convertLevelColorsSettingsToColorProcessor,
  defaultDigitalSimpleGaugeOptions,
  digitalGaugeLayoutImages,
  digitalGaugeLayouts,
  digitalGaugeLayoutTranslations,
  DigitalGaugeSettings,
  DigitalGaugeType
} from '@home/components/widget/lib/digital-gauge.models';
import { ColorSettings, ColorType } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-digital-simple-gauge-basic-config',
  templateUrl: './digital-simple-gauge-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})

export class DigitalSimpleGaugeBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.simpleGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.simpleGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  digitalGaugeType = DigitalGaugeType;
  digitalGaugeLayouts = digitalGaugeLayouts;

  digitalGaugeLayoutTranslationMap = digitalGaugeLayoutTranslations;
  digitalGaugeLayoutImageMap = digitalGaugeLayoutImages;

  simpleGaugeWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this, true);
  previewFn = this._valuePreviewFn.bind(this, false);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.simpleGaugeWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    super.setupDefaults(configData);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: DigitalGaugeSettings = {...defaultDigitalSimpleGaugeOptions, ...(configData.config.settings || {})};

    convertLevelColorsSettingsToColorProcessor(settings, settings.defaultColor || '#2196f3');

    this.simpleGaugeWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      gaugeType: [settings.gaugeType, []],
      donutStartAngle: [settings.donutStartAngle, []],

      showMinMax: [settings.showMinMax, []],
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, [this.maxValueValidation()]],
      minMaxFont: [settings.minMaxFont, []],
      minMaxColor: [settings.minMaxFont?.color, []],

      showValue: [settings.showValue, []],
      decimals: [isDefined(configData.config.decimals) ? configData.config.decimals : settings.decimals, []],
      units: [configData.config.units, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueFont?.color, []],

      showTitle: [settings.showTitle, []],
      title: [settings.title, []],
      titleFont: [settings.titleFont, []],
      titleColor: [settings.titleFont?.color, []],

      gaugeColor: [settings.gaugeColor, []],
      barColor: [settings.barColor, []],

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

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;

    this.widgetConfig.config.settings.gaugeType = config.gaugeType;
    this.widgetConfig.config.settings.donutStartAngle = config.donutStartAngle;

    this.widgetConfig.config.settings.showMinMax = config.showMinMax;
    this.widgetConfig.config.settings.minValue = config.minValue;
    this.widgetConfig.config.settings.maxValue = config.maxValue;
    this.widgetConfig.config.settings.minMaxFont = config.minMaxFont;
    this.widgetConfig.config.settings.minMaxFont.color = config.minMaxColor;

    this.widgetConfig.config.settings.showValue = config.showValue;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    if (isDefined(this.widgetConfig.config.settings.decimals)) {
      delete this.widgetConfig.config.settings.decimals;
    }
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueFont.color = config.valueColor;

    this.widgetConfig.config.settings.showTitle = config.showTitle;
    this.widgetConfig.config.settings.title = config.title;
    this.widgetConfig.config.settings.titleFont = config.titleFont;
    this.widgetConfig.config.settings.titleFont.color = config.titleColor;

    this.widgetConfig.config.settings.gaugeColor = config.gaugeColor;
    this.widgetConfig.config.settings.barColor = config.barColor;
    const barColor: ColorSettings = config.barColor;

    if (barColor.type === ColorType.range) {
      this.widgetConfig.config.settings.useFixedLevelColor = true;
      this.widgetConfig.config.settings.fixedLevelColors =
        barColor.rangeList.advancedMode ? barColor.rangeList.rangeAdvanced : barColor.rangeList.range;
    } else {
      this.widgetConfig.config.settings.useFixedLevelColor = false;
    }
    if (barColor.gradient?.gradient?.length) {
      this.widgetConfig.config.settings.levelColors = barColor.gradient.gradient;
    }

    return this.widgetConfig;
  }

  private maxValueValidation(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: string = control.value;
      if (value) {
        if (value < control.parent?.get('minValue').value) {
          return {maxValue: true};
        }
      }
      return null;
    };
  }

  protected validatorTriggers(): string[] {
    return ['gaugeType', 'showValue', 'showTitle', 'showMinMax', 'minValue'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    if (trigger === 'minValue') {
      this.simpleGaugeWidgetConfigForm.get('maxValue').updateValueAndValidity({emitEvent: true});
      this.simpleGaugeWidgetConfigForm.get('maxValue').markAsTouched({onlySelf: true});
      return;
    }

    const isDonut = this.simpleGaugeWidgetConfigForm.get('gaugeType').value === this.digitalGaugeType.donut;

    if (isDonut) {
      this.simpleGaugeWidgetConfigForm.get('donutStartAngle').enable();
      this.simpleGaugeWidgetConfigForm.get('showMinMax').disable({emitEvent: false});
      this.simpleGaugeWidgetConfigForm.get('minValue').enable();
      this.simpleGaugeWidgetConfigForm.get('maxValue').enable();
      this.simpleGaugeWidgetConfigForm.get('minMaxFont').disable();
      this.simpleGaugeWidgetConfigForm.get('minMaxColor').disable();
    } else {
      this.simpleGaugeWidgetConfigForm.get('donutStartAngle').disable();
      this.simpleGaugeWidgetConfigForm.get('showMinMax').enable({emitEvent: false});
      if (this.simpleGaugeWidgetConfigForm.get('showMinMax').value) {
        this.simpleGaugeWidgetConfigForm.get('minValue').enable();
        this.simpleGaugeWidgetConfigForm.get('maxValue').enable();
        this.simpleGaugeWidgetConfigForm.get('minMaxFont').enable();
        this.simpleGaugeWidgetConfigForm.get('minMaxColor').enable();
      } else {
        this.simpleGaugeWidgetConfigForm.get('minValue').disable();
        this.simpleGaugeWidgetConfigForm.get('maxValue').disable();
        this.simpleGaugeWidgetConfigForm.get('minMaxFont').disable();
        this.simpleGaugeWidgetConfigForm.get('minMaxColor').disable();
      }
    }

    if (this.simpleGaugeWidgetConfigForm.get('showValue').value) {
      this.simpleGaugeWidgetConfigForm.get('decimals').enable();
      this.simpleGaugeWidgetConfigForm.get('units').enable();
      this.simpleGaugeWidgetConfigForm.get('valueFont').enable();
      this.simpleGaugeWidgetConfigForm.get('valueColor').enable();
    } else {
      this.simpleGaugeWidgetConfigForm.get('decimals').disable();
      this.simpleGaugeWidgetConfigForm.get('units').disable();
      this.simpleGaugeWidgetConfigForm.get('valueFont').disable();
      this.simpleGaugeWidgetConfigForm.get('valueColor').disable();
    }

    if (this.simpleGaugeWidgetConfigForm.get('showTitle').value) {
      this.simpleGaugeWidgetConfigForm.get('title').enable();
      this.simpleGaugeWidgetConfigForm.get('titleFont').enable();
      this.simpleGaugeWidgetConfigForm.get('titleColor').enable();
    } else {
      this.simpleGaugeWidgetConfigForm.get('title').disable();
      this.simpleGaugeWidgetConfigForm.get('titleFont').disable();
      this.simpleGaugeWidgetConfigForm.get('titleColor').disable();
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

  private _valuePreviewFn(units: boolean): string {
    return formatValue(22, 0, units ? getSourceTbUnitSymbol(this.simpleGaugeWidgetConfigForm.get('units').value) : null, true);
  }
}
