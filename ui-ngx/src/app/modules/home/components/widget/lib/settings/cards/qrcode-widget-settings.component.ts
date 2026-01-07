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
  selector: 'tb-qrcode-widget-settings',
  templateUrl: './qrcode-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class QrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  qrCodeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.qrCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      qrCodeTextPattern: '${entityName}',
      useQrCodeTextFunction: false,
      qrCodeTextFunction: 'return data[0][\'entityName\'];'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.qrCodeWidgetSettingsForm = this.fb.group({
      qrCodeTextPattern: [settings.qrCodeTextPattern, [Validators.required]],
      useQrCodeTextFunction: [settings.useQrCodeTextFunction, [Validators.required]],
      qrCodeTextFunction: [settings.qrCodeTextFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useQrCodeTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useQrCodeTextFunction: boolean = this.qrCodeWidgetSettingsForm.get('useQrCodeTextFunction').value;
    if (useQrCodeTextFunction) {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').disable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').enable();
    } else {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').enable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').disable();
    }
    this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').updateValueAndValidity({emitEvent});
    this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').updateValueAndValidity({emitEvent});
  }

}
