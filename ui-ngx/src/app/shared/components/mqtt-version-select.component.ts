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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { SubscriptSizing, MatFormFieldAppearance } from '@angular/material/form-field';
import { MqttVersionTranslation, MqttVersion } from '@shared/models/mqtt.models';

@Component({
  selector: 'tb-mqtt-version-select',
  templateUrl: './mqtt-version-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MqttVersionSelectComponent),
    multi: true
  }]
})
export class MqttVersionSelectComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'dynamic';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  mqttVersions = Object.values(MqttVersion);
  mqttVersionTranslation = MqttVersionTranslation;
  modelValue: MqttVersion;

  @Input()
  @coerceBoolean()
  required = false;

  private propagateChange = (v: any) => { };

  constructor() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: MqttVersion | null): void {
    this.modelValue = value;
  }

  mqttVersionChanged() {
    this.updateView();
  }

  private updateView() {
    this.propagateChange(this.modelValue);
  }
}
