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
import { flotDataKeyDefaultSettings } from '@home/components/widget/lib/settings/chart/flot-key-settings.component';

@Component({
  selector: 'tb-flot-bar-key-settings',
  templateUrl: './flot-bar-key-settings.component.html',
  styleUrls: []
})
export class FlotBarKeySettingsComponent extends WidgetSettingsComponent {

  flotBarKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotBarKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return flotDataKeyDefaultSettings('bar');
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotBarKeySettingsForm = this.fb.group({
      flotKeySettings: [settings.flotKeySettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      flotKeySettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.flotKeySettings;
  }
}
