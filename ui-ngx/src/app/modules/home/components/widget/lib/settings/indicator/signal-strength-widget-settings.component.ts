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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import {
  signalStrengthDefaultSettings,
  signalStrengthLayoutImages,
  signalStrengthLayouts,
  signalStrengthLayoutTranslations
} from '@home/components/widget/lib/indicator/signal-strength-widget.models';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-signal-strength-widget-settings',
  templateUrl: './signal-strength-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
})
export class SignalStrengthWidgetSettingsComponent extends WidgetSettingsComponent {

  signalStrengthLayouts = signalStrengthLayouts;

  signalStrengthLayoutTranslationMap = signalStrengthLayoutTranslations;
  signalStrengthLayoutImageMap = signalStrengthLayoutImages;

  signalStrengthWidgetSettingsForm: UntypedFormGroup;

  datePreviewFn = this._datePreviewFn.bind(this);
  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);
  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.signalStrengthWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return signalStrengthDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.signalStrengthWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      activeBarsColor: [settings.activeBarsColor, []],
      inactiveBarsColor: [settings.inactiveBarsColor, []],

      showTooltip: [settings.showTooltip, []],

      showTooltipValue: [settings.showTooltipValue, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],

      showTooltipDate: [settings.showTooltipDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []],
      padding: [settings.padding, []],
      noSignalRssiValue: [settings.noSignalRssiValue, [Validators.max(-86)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showDate', 'showTooltip', 'showTooltipValue', 'showTooltipDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showDate: boolean = this.signalStrengthWidgetSettingsForm.get('showDate').value;
    const showTooltip: boolean = this.signalStrengthWidgetSettingsForm.get('showTooltip').value;
    const showTooltipValue: boolean = this.signalStrengthWidgetSettingsForm.get('showTooltipValue').value;
    const showTooltipDate: boolean = this.signalStrengthWidgetSettingsForm.get('showTooltipDate').value;

    if (showDate) {
      this.signalStrengthWidgetSettingsForm.get('dateFormat').enable();
      this.signalStrengthWidgetSettingsForm.get('dateFont').enable();
      this.signalStrengthWidgetSettingsForm.get('dateColor').enable();
    } else {
      this.signalStrengthWidgetSettingsForm.get('dateFormat').disable();
      this.signalStrengthWidgetSettingsForm.get('dateFont').disable();
      this.signalStrengthWidgetSettingsForm.get('dateColor').disable();
    }

    if (showTooltip) {
      this.signalStrengthWidgetSettingsForm.get('showTooltipValue').enable({emitEvent: false});
      this.signalStrengthWidgetSettingsForm.get('showTooltipDate').enable({emitEvent: false});
      this.signalStrengthWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.signalStrengthWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      if (showTooltipValue) {
        this.signalStrengthWidgetSettingsForm.get('tooltipValueFont').enable();
        this.signalStrengthWidgetSettingsForm.get('tooltipValueColor').enable();
      } else {
        this.signalStrengthWidgetSettingsForm.get('tooltipValueFont').disable();
        this.signalStrengthWidgetSettingsForm.get('tooltipValueColor').disable();
      }
      if (showTooltipDate) {
        this.signalStrengthWidgetSettingsForm.get('tooltipDateFormat').enable();
        this.signalStrengthWidgetSettingsForm.get('tooltipDateFont').enable();
        this.signalStrengthWidgetSettingsForm.get('tooltipDateColor').enable();
      } else {
        this.signalStrengthWidgetSettingsForm.get('tooltipDateFormat').disable();
        this.signalStrengthWidgetSettingsForm.get('tooltipDateFont').disable();
        this.signalStrengthWidgetSettingsForm.get('tooltipDateColor').disable();
      }
    } else {
      this.signalStrengthWidgetSettingsForm.get('showTooltipValue').disable({emitEvent: false});
      this.signalStrengthWidgetSettingsForm.get('showTooltipDate').disable({emitEvent: false});
      this.signalStrengthWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipValueFont').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipValueColor').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipDateFont').disable();
      this.signalStrengthWidgetSettingsForm.get('tooltipDateColor').disable();
    }
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.signalStrengthWidgetSettingsForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

  private _tooltipValuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(-76, decimals, units, true);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.signalStrengthWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, {...dateFormat, ...{hideLastUpdatePrefix: true}});
    processor.update(Date.now());
    return processor.formatted;
  }

}
