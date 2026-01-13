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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  updateAttributeGeneralDefaultSettings
} from '@home/components/widget/lib/settings/input/update-attribute-general-settings.component';

@Component({
  selector: 'tb-update-date-attribute-widget-settings',
  templateUrl: './update-date-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateDateAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateDateAttributeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateDateAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...updateAttributeGeneralDefaultSettings(false),
      showTimeInput: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateDateAttributeWidgetSettingsForm = this.fb.group({
      updateAttributeGeneralSettings: [settings.updateAttributeGeneralSettings, []],
      showTimeInput: [settings.showTimeInput, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const updateAttributeGeneralSettings = deepClone(settings, ['showTimeInput']);
    return {
      updateAttributeGeneralSettings,
      showTimeInput: settings.showTimeInput
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.updateAttributeGeneralSettings,
      showTimeInput: settings.showTimeInput
    };
  }
}
