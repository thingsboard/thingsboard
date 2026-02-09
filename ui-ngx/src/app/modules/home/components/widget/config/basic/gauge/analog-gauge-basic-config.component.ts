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
import { formatValue, isUndefined } from '@core/utils';
import { ColorType } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

export class GaugeBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.radialGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.radialGaugeWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  colorType = ColorType;

  radialGaugeWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);
  unitsPreviewFn = this._unitsPreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.radialGaugeWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    super.setupDefaults(configData);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.radialGaugeWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      showUnitTitle: [configData.config.settings?.showUnitTitle, []],
      unitTitle: [configData.config.settings?.unitTitle, []],
      titleFont: [configData.config.settings?.titleFont, []],
      titleColor: [configData.config.settings?.titleFont?.color, []],

      units: [configData.config.units, []],
      unitsFont: [configData.config.settings?.unitsFont, []],
      unitsColor: [configData.config.settings?.unitsFont?.color, []],

      valueBox: [configData.config.settings?.valueBox, []],
      valueInt: [configData.config.settings?.valueInt, []],
      valueFont: [configData.config.settings?.valueFont, []],
      valueColor: [configData.config.settings?.valueFont?.color, []],

      minValue: [configData.config.settings?.minValue, []],
      maxValue: [configData.config.settings?.maxValue, []],
      numbersFont: [configData.config.settings?.numbersFont, []],
      numbersColor: [configData.config.settings?.numbersFont?.color, []],

      colorBarStroke: [configData.config.settings?.colorBarStroke, []],

      defaultColor: [configData.config.settings?.defaultColor, []],
      colorPlate: [configData.config.settings?.colorPlate, []],
      highlights: [configData.config.settings?.highlights, []],

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

    this.widgetConfig.config.settings.showUnitTitle = config.showUnitTitle;
    this.widgetConfig.config.settings.unitTitle = config.unitTitle;
    this.widgetConfig.config.settings.titleFont = config.titleFont;
    this.widgetConfig.config.settings.titleFont.color = config.titleColor;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.settings.unitsFont = config.unitsFont;
    this.widgetConfig.config.settings.unitsFont.color = config.unitsColor;

    this.widgetConfig.config.settings.valueBox = config.valueBox;
    this.widgetConfig.config.settings.valueInt = config.valueInt;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueFont.color = config.valueColor;

    this.widgetConfig.config.settings.minValue = config.minValue;
    this.widgetConfig.config.settings.maxValue = config.maxValue;
    this.widgetConfig.config.settings.numbersFont = config.numbersFont;
    this.widgetConfig.config.settings.numbersFont.color = config.numbersColor;

    this.widgetConfig.config.settings.defaultColor = config.defaultColor;
    this.widgetConfig.config.settings.colorPlate = config.colorPlate;
    this.widgetConfig.config.settings.highlights = config.highlights;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showUnitTitle', 'valueBox'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showUnitTitle: boolean = this.radialGaugeWidgetConfigForm.get('showUnitTitle').value;
    const valueBox: boolean = this.radialGaugeWidgetConfigForm.get('valueBox').value;
    if (showUnitTitle) {
      this.radialGaugeWidgetConfigForm.get('unitTitle').enable();
      this.radialGaugeWidgetConfigForm.get('titleFont').enable();
      this.radialGaugeWidgetConfigForm.get('titleColor').enable();
    } else {
      this.radialGaugeWidgetConfigForm.get('unitTitle').disable();
      this.radialGaugeWidgetConfigForm.get('titleFont').disable();
      this.radialGaugeWidgetConfigForm.get('titleColor').disable();
    }
    if (valueBox) {
      this.radialGaugeWidgetConfigForm.get('valueInt').enable();
      this.radialGaugeWidgetConfigForm.get('valueFont').enable();
      this.radialGaugeWidgetConfigForm.get('valueColor').enable();
    } else {
      this.radialGaugeWidgetConfigForm.get('valueInt').disable();
      this.radialGaugeWidgetConfigForm.get('valueFont').disable();
      this.radialGaugeWidgetConfigForm.get('valueColor').disable();
    }
    this.radialGaugeWidgetConfigForm.get('unitTitle').updateValueAndValidity({emitEvent});
    this.radialGaugeWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.radialGaugeWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.radialGaugeWidgetConfigForm.get('valueInt').updateValueAndValidity({emitEvent});
    this.radialGaugeWidgetConfigForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.radialGaugeWidgetConfigForm.get('valueColor').updateValueAndValidity({emitEvent});
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

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.radialGaugeWidgetConfigForm.get('units').value);
    return formatValue(22, 0, units, true);
  }

  private _unitsPreviewFn(): string {
    return getSourceTbUnitSymbol(this.radialGaugeWidgetConfigForm.get('units').value);
  }
}
