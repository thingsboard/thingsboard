// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
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
        }],
    standalone: false
})
export class MqttVersionSelectComponent implements ControlValueAccessor, OnChanges {

  @Input()
  disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'dynamic';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  excludeVersions: MqttVersion[];

  mqttVersions =  Object.values(MqttVersion);
  mqttVersionTranslation = MqttVersionTranslation;
  modelValue: MqttVersion;

  @Input()
  @coerceBoolean()
  required = false;

  private propagateChange = (v: any) => { };

  constructor() {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (propName === 'excludeVersions' && change.currentValue !== change.previousValue) {
        const excludeVersions = change.currentValue;
        if (excludeVersions?.length) {
          this.mqttVersions = Object.values(MqttVersion).filter(v => !excludeVersions.includes(v));
        } else {
          this.mqttVersions = Object.values(MqttVersion);
        }
      }
    }
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
