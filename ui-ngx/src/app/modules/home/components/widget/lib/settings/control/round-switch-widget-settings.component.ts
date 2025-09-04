///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { TargetDevice, WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { switchRpcDefaultSettings } from '@home/components/widget/lib/settings/control/switch-rpc-settings.component';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-round-switch-widget-settings',
  templateUrl: './round-switch-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class RoundSwitchWidgetSettingsComponent extends WidgetSettingsComponent {

  roundSwitchWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  protected settingsForm(): UntypedFormGroup {
    return this.roundSwitchWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      ...switchRpcDefaultSettings()
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.roundSwitchWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      switchRpcSettings: [settings.switchRpcSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const switchRpcSettings = deepClone(settings, ['title']);
    return {
      title: settings.title,
      switchRpcSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.switchRpcSettings,
      title: settings.title
    };
  }
}
