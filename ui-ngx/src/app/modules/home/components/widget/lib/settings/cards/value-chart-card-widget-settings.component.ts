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

import { Component, Injector } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  valueCartCardLayouts,
  valueChartCardDefaultSettings,
  valueChartCardLayoutImages,
  valueChartCardLayoutTranslations
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-value-chart-card-widget-settings',
    templateUrl: './value-chart-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class ValueChartCardWidgetSettingsComponent extends WidgetSettingsComponent {

  valueChartCardLayouts = valueCartCardLayouts;

  valueChartCardLayoutTranslationMap = valueChartCardLayoutTranslations;
  valueChartCardLayoutImageMap = valueChartCardLayoutImages;

  valueChartCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  datePreviewFn = this._datePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.valueChartCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return valueChartCardDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.valueChartCardWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue', 'showDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.valueChartCardWidgetSettingsForm.get('showValue').value;
    const showDate: boolean = this.valueChartCardWidgetSettingsForm.get('showDate').value;

    if (showValue) {
      this.valueChartCardWidgetSettingsForm.get('valueFont').enable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.valueChartCardWidgetSettingsForm.get('valueFont').disable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').disable();
    }

    if (showDate) {
      this.valueChartCardWidgetSettingsForm.get('dateFormat').enable();
      this.valueChartCardWidgetSettingsForm.get('dateFont').enable();
      this.valueChartCardWidgetSettingsForm.get('dateColor').enable();
    } else {
      this.valueChartCardWidgetSettingsForm.get('dateFormat').disable();
      this.valueChartCardWidgetSettingsForm.get('dateFont').disable();
      this.valueChartCardWidgetSettingsForm.get('dateColor').disable();
    }

    this.valueChartCardWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('dateFormat').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('dateFont').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('dateColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.valueChartCardWidgetSettingsForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

}
