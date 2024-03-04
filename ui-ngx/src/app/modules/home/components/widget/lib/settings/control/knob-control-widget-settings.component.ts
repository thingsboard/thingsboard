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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-knob-control-widget-settings',
  templateUrl: './knob-control-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class KnobControlWidgetSettingsComponent extends WidgetSettingsComponent {

  knobControlWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.knobControlWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      minValue: 0,
      maxValue: 100,
      initialValue: 50,
      getValueMethod: 'getValue',
      setValueMethod: 'setValue',
      requestTimeout: 500,
      requestPersistent: false,
      persistentPollingInterval: 5000
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.knobControlWidgetSettingsForm = this.fb.group({

      // Common settings

      title: [settings.title, []],

      // Value settings

      initialValue: [settings.initialValue, []],
      minValue: [settings.minValue, [Validators.required]],
      maxValue: [settings.maxValue, [Validators.required]],

      getValueMethod: [settings.getValueMethod, [Validators.required]],
      setValueMethod: [settings.setValueMethod, [Validators.required]],

      // RPC settings

      requestTimeout: [settings.requestTimeout, [Validators.min(0), Validators.required]],

      // --> Persistent RPC settings

      requestPersistent: [settings.requestPersistent, []],
      persistentPollingInterval: [settings.persistentPollingInterval, [Validators.min(1000)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['requestPersistent'];
  }

  protected updateValidators(emitEvent: boolean): void {
    const requestPersistent: boolean = this.knobControlWidgetSettingsForm.get('requestPersistent').value;
    if (requestPersistent) {
      this.knobControlWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
    } else {
      this.knobControlWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.knobControlWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}
