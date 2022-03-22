///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, ViewChild } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { AbstractControl, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { JsFuncComponent } from '@shared/components/js-func.component';

@Component({
  selector: 'tb-qrcode-widget-settings',
  templateUrl: './qrcode-widget-settings.component.html',
  styleUrls: []
})
export class QrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('qrCodeTextFunctionComponent', {static: true}) qrCodeTextFunctionComponent: JsFuncComponent;

  qrCodeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
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
      qrCodeTextPattern: [settings.qrCodeTextPattern, []],
      useQrCodeTextFunction: [settings.useQrCodeTextFunction, []],
      qrCodeTextFunction: [settings.qrCodeTextFunction, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useQrCodeTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useQrCodeTextFunction: boolean = this.qrCodeWidgetSettingsForm.get('useQrCodeTextFunction').value;
    if (useQrCodeTextFunction) {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').setValidators([]);
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').setValidators([Validators.required,
        (control: AbstractControl) => this.qrCodeTextFunctionComponent.validate(control as FormControl)
      ]);
    } else {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').setValidators([Validators.required]);
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').setValidators([]);
    }
    this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').updateValueAndValidity({emitEvent});
    this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').updateValueAndValidity({emitEvent});
  }

}
