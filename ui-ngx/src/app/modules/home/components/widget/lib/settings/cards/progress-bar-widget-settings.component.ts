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
import {
  progressBarDefaultSettings,
  ProgressBarLayout,
  progressBarLayoutImages,
  progressBarLayouts,
  progressBarLayoutTranslations
} from '@home/components/widget/lib/cards/progress-bar-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-progress-bar-widget-settings',
    templateUrl: './progress-bar-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class ProgressBarWidgetSettingsComponent extends WidgetSettingsComponent {

  progressBarLayout = ProgressBarLayout;

  progressBarLayouts = progressBarLayouts;

  progressBarLayoutTranslationMap = progressBarLayoutTranslations;
  progressBarLayoutImageMap = progressBarLayoutImages;

  progressBarWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.progressBarWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return progressBarDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.progressBarWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      tickMin: [settings.tickMin, []],
      tickMax: [settings.tickMax, []],

      showTicks: [settings.showTicks, []],
      ticksFont: [settings.ticksFont, []],
      ticksColor: [settings.ticksColor, []],

      barColor: [settings.barColor, []],
      barBackground: [settings.barBackground, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue', 'showTicks', 'layout'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.progressBarWidgetSettingsForm.get('showValue').value;
    const showTicks: boolean = this.progressBarWidgetSettingsForm.get('showTicks').value;
    const layout: ProgressBarLayout = this.progressBarWidgetSettingsForm.get('layout').value;

    const ticksEnabled = layout === ProgressBarLayout.default;

    if (showValue) {
      this.progressBarWidgetSettingsForm.get('valueFont').enable();
      this.progressBarWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.progressBarWidgetSettingsForm.get('valueFont').disable();
      this.progressBarWidgetSettingsForm.get('valueColor').disable();
    }

    if (ticksEnabled) {
      this.progressBarWidgetSettingsForm.get('showTicks').enable({emitEvent: false});
      if (showTicks) {
        this.progressBarWidgetSettingsForm.get('ticksFont').enable();
        this.progressBarWidgetSettingsForm.get('ticksColor').enable();
      } else {
        this.progressBarWidgetSettingsForm.get('ticksFont').disable();
        this.progressBarWidgetSettingsForm.get('ticksColor').disable();
      }
    } else {
      this.progressBarWidgetSettingsForm.get('showTicks').disable({emitEvent: false});
      this.progressBarWidgetSettingsForm.get('ticksFont').disable();
      this.progressBarWidgetSettingsForm.get('ticksColor').disable();
    }

    this.progressBarWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('showTicks').updateValueAndValidity({emitEvent: false});
    this.progressBarWidgetSettingsForm.get('ticksFont').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('ticksColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(78, decimals, units, true);
  }

}
