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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-date-range-navigator-widget-settings',
    templateUrl: './date-range-navigator-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class DateRangeNavigatorWidgetSettingsComponent extends WidgetSettingsComponent {

  dateRangeNavigatorWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.dateRangeNavigatorWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      hidePicker: false,
      onePanel: false,
      autoConfirm: false,
      showTemplate: false,
      firstDayOfWeek: 1,
      hideInterval: false,
      initialInterval: 'week',
      hideStepSize: false,
      stepSize: 'day',
      hideLabels: false,
      useSessionStorage: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.dateRangeNavigatorWidgetSettingsForm = this.fb.group({

      // Date range picker settings

      hidePicker: [settings.hidePicker, []],
      onePanel: [settings.onePanel, []],
      autoConfirm: [settings.autoConfirm, []],
      showTemplate: [settings.showTemplate, []],
      firstDayOfWeek: [settings.firstDayOfWeek, [Validators.min(1), Validators.max(7)]],

      // Interval settings

      hideInterval: [settings.hideInterval, []],
      initialInterval: [settings.initialInterval, []],

      // Step settings

      hideStepSize: [settings.hideStepSize, []],
      stepSize: [settings.stepSize, []],

      hideLabels: [settings.hideLabels, []],
      useSessionStorage: [settings.useSessionStorage, []],
    });
  }

  protected validatorTriggers(): string[] {
    return ['hidePicker', 'hideInterval', 'hideStepSize'];
  }

  protected updateValidators(emitEvent: boolean) {
    const hidePicker: boolean = this.dateRangeNavigatorWidgetSettingsForm.get('hidePicker').value;
    const hideInterval: boolean = this.dateRangeNavigatorWidgetSettingsForm.get('hideInterval').value;
    const hideStepSize: boolean = this.dateRangeNavigatorWidgetSettingsForm.get('hideStepSize').value;
    if (hidePicker) {
      this.dateRangeNavigatorWidgetSettingsForm.get('onePanel').disable();
      this.dateRangeNavigatorWidgetSettingsForm.get('autoConfirm').disable();
      this.dateRangeNavigatorWidgetSettingsForm.get('showTemplate').disable();
      this.dateRangeNavigatorWidgetSettingsForm.get('firstDayOfWeek').disable();
    } else {
      this.dateRangeNavigatorWidgetSettingsForm.get('onePanel').enable();
      this.dateRangeNavigatorWidgetSettingsForm.get('autoConfirm').enable();
      this.dateRangeNavigatorWidgetSettingsForm.get('showTemplate').enable();
      this.dateRangeNavigatorWidgetSettingsForm.get('firstDayOfWeek').enable();
    }
    if (hideInterval) {
      this.dateRangeNavigatorWidgetSettingsForm.get('initialInterval').disable();
    } else {
      this.dateRangeNavigatorWidgetSettingsForm.get('initialInterval').enable();
    }
    if (hideStepSize) {
      this.dateRangeNavigatorWidgetSettingsForm.get('stepSize').disable();
    } else {
      this.dateRangeNavigatorWidgetSettingsForm.get('stepSize').enable();
    }
    this.dateRangeNavigatorWidgetSettingsForm.get('onePanel').updateValueAndValidity({emitEvent});
    this.dateRangeNavigatorWidgetSettingsForm.get('autoConfirm').updateValueAndValidity({emitEvent});
    this.dateRangeNavigatorWidgetSettingsForm.get('showTemplate').updateValueAndValidity({emitEvent});
    this.dateRangeNavigatorWidgetSettingsForm.get('firstDayOfWeek').updateValueAndValidity({emitEvent});
    this.dateRangeNavigatorWidgetSettingsForm.get('initialInterval').updateValueAndValidity({emitEvent});
    this.dateRangeNavigatorWidgetSettingsForm.get('stepSize').updateValueAndValidity({emitEvent});
  }

}
