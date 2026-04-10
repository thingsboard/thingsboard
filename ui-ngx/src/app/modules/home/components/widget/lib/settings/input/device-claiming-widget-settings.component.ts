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
    selector: 'tb-device-claiming-widget-settings',
    templateUrl: './device-claiming-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class DeviceClaimingWidgetSettingsComponent extends WidgetSettingsComponent {

  deviceClaimingWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.deviceClaimingWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      labelClaimButon: '',
      deviceSecret: false,
      showLabel: true,
      deviceLabel: '',
      secretKeyLabel: '',
      successfulClaimDevice: '',
      deviceNotFound: '',
      failedClaimDevice: '',
      requiredErrorDevice: '',
      requiredErrorSecretKey: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.deviceClaimingWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      labelClaimButon: [settings.labelClaimButon, []],
      deviceSecret: [settings.deviceSecret, []],

      // Labels settings

      showLabel: [settings.showLabel, []],
      deviceLabel: [settings.deviceLabel, []],
      secretKeyLabel: [settings.secretKeyLabel, []],

      // Message settings

      successfulClaimDevice: [settings.successfulClaimDevice, []],
      deviceNotFound: [settings.deviceNotFound, []],
      failedClaimDevice: [settings.failedClaimDevice, []],
      requiredErrorDevice: [settings.requiredErrorDevice, []],
      requiredErrorSecretKey: [settings.requiredErrorSecretKey, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['deviceSecret', 'showLabel'];
  }

  protected updateValidators(emitEvent: boolean) {
    const deviceSecret: boolean = this.deviceClaimingWidgetSettingsForm.get('deviceSecret').value;
    const showLabel: boolean = this.deviceClaimingWidgetSettingsForm.get('showLabel').value;
    if (deviceSecret) {
      if (showLabel) {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').enable();
      } else {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
      }
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').disable();
      this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
    }
    if (showLabel) {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').disable();
    }
    this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('deviceLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').updateValueAndValidity({emitEvent});
  }

}
