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
  selector: 'tb-navigation-card-widget-settings',
  templateUrl: './navigation-card-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class NavigationCardWidgetSettingsComponent extends WidgetSettingsComponent {

  navigationCardWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.navigationCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      name: '{i18n:device.devices}',
      icon: 'devices_other',
      path: '/devices'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.navigationCardWidgetSettingsForm = this.fb.group({
      name: [settings.name, [Validators.required]],
      icon: [settings.icon, [Validators.required]],
      path: [settings.path, [Validators.required]]
    });
  }
}
