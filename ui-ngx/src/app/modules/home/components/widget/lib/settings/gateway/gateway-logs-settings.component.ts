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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-gateway-logs-settings',
  templateUrl: './gateway-logs-settings.component.html',
  styleUrls: ['../widget-settings.scss']
})
export class GatewayLogsSettingsComponent extends WidgetSettingsComponent {

  gatewayLogSettingForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayLogSettingForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      isConnectorLog: false,
      connectorLogState: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayLogSettingForm = this.fb.group({
      isConnectorLog: [false, []],
      connectorLogState: ['', Validators.required]
    });
  }

  protected validatorTriggers(): string[] {
    return ['isConnectorLog'];
  }

  protected updateValidators(emitEvent: boolean) {
    const isConnectorLog: boolean = this.gatewayLogSettingForm.get('isConnectorLog').value;
    if (isConnectorLog) {
      this.gatewayLogSettingForm.get('connectorLogState').enable({emitEvent});
    } else {
      this.gatewayLogSettingForm.get('connectorLogState').disable({emitEvent});
    }
  }
}
