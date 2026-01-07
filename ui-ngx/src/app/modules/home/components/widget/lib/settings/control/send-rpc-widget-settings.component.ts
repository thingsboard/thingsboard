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
import { ContentType } from '@shared/models/constants';

@Component({
  selector: 'tb-send-rpc-widget-settings',
  templateUrl: './send-rpc-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class SendRpcWidgetSettingsComponent extends WidgetSettingsComponent {

  sendRpcWidgetSettingsForm: UntypedFormGroup;

  contentTypes = ContentType;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.sendRpcWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      buttonText: 'Send RPC',
      oneWayElseTwoWay: true,
      showError: false,
      methodName: 'rpcCommand',
      methodParams: '{}',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 5000,
      styleButton: {
        isRaised: true,
        isPrimary: false,
        bgColor: null,
        textColor: null
      }
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.sendRpcWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      buttonText: [settings.buttonText, []],

      // RPC settings

      oneWayElseTwoWay: [settings.oneWayElseTwoWay, []],
      methodName: [settings.methodName, []],
      methodParams: [settings.methodParams, []],
      requestTimeout: [settings.requestTimeout, [Validators.min(0)]],
      showError: [settings.showError, []],

      // Persistent RPC settings

      requestPersistent: [settings.requestPersistent, []],
      persistentPollingInterval: [settings.persistentPollingInterval, [Validators.min(1000)]],

      styleButton: [settings.styleButton, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['requestPersistent'];
  }

  protected updateValidators(emitEvent: boolean): void {
    const requestPersistent: boolean = this.sendRpcWidgetSettingsForm.get('requestPersistent').value;
    if (requestPersistent) {
      this.sendRpcWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
    } else {
      this.sendRpcWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.sendRpcWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}
