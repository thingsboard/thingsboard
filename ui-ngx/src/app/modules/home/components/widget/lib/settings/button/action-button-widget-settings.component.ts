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
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import { actionButtonDefaultSettings } from '@home/components/widget/lib/button/action-button-widget.models';

@Component({
  selector: 'tb-action-button-widget-settings',
  templateUrl: './action-button-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class ActionButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    const datasources = this.widgetConfig?.config?.datasources;
    return getTargetDeviceFromDatasources(datasources);
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }
  get borderRadius(): string {
    return this.widgetConfig?.config?.borderRadius;
  }

  valueType = ValueType;

  actionButtonWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.actionButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return actionButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.actionButtonWidgetSettingsForm = this.fb.group({
      activatedState: [settings.activatedState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []]
    });
  }
}
