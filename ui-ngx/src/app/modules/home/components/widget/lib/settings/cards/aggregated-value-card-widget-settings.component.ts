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
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import { aggregatedValueCardDefaultSettings } from '@home/components/widget/lib/cards/aggregated-value-card.models';

@Component({
  selector: 'tb-aggregated-value-card-widget-settings',
  templateUrl: './aggregated-value-card-widget-settings.component.html',
  styleUrls: []
})
export class AggregatedValueCardWidgetSettingsComponent extends WidgetSettingsComponent {

  aggregatedValueCardWidgetSettingsForm: UntypedFormGroup;

  datePreviewFn = this._datePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.aggregatedValueCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return aggregatedValueCardDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.aggregatedValueCardWidgetSettingsForm = this.fb.group({

      autoScale: [settings.autoScale, []],
      showSubtitle: [settings.showSubtitle, []],
      subtitle: [settings.subtitle, []],
      subtitleFont: [settings.subtitleFont, []],
      subtitleColor: [settings.subtitleColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      showChart: [settings.showChart, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showSubtitle', 'showDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showSubtitle: boolean = this.aggregatedValueCardWidgetSettingsForm.get('showSubtitle').value;
    const showDate: boolean = this.aggregatedValueCardWidgetSettingsForm.get('showDate').value;

    if (showSubtitle) {
      this.aggregatedValueCardWidgetSettingsForm.get('subtitle').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').enable();
    } else {
      this.aggregatedValueCardWidgetSettingsForm.get('subtitle').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').disable();
    }

    if (showDate) {
      this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateFont').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateColor').enable();
    } else {
      this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateFont').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateColor').disable();
    }

    this.aggregatedValueCardWidgetSettingsForm.get('subtitle').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateFont').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateColor').updateValueAndValidity({emitEvent});
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
