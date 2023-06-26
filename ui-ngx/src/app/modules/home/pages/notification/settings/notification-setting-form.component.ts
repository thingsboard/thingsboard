///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { UtilsService } from '@core/services/utils.service';
import { isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';
import { NotificationDeliveryMethod, NotificationUserSetting } from '@shared/models/notification.models';

@Component({
  selector: 'tb-notification-setting-form',
  templateUrl: './notification-setting-form.component.html',
  styleUrls: ['./notification-setting-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NotificationSettingFormComponent),
      multi: true
    }
  ]
})
export class NotificationSettingFormComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  allowDeliveryMethods = [];

  notificationSettingsFormGroup: UntypedFormGroup;

  notificationDeliveryMethod = NotificationDeliveryMethod;
  notificationDeliveryMethodMap = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];

  private modelValue;
  private propagateChange = null;
  private propagateChangePending = false;
  private valueChange$: Subscription = null;

  constructor(private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.modelValue);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.notificationSettingsFormGroup = this.fb.group(
      {
        ruleId: [],
        ruleName: [''],
        enabled: [true],
        enabledDeliveryMethods: []
      });
    this.valueChange$ = this.notificationSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
      this.valueChange$ = null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.notificationSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.notificationSettingsFormGroup.enable({emitEvent: false});
    }
  }

  toggleEnabled() {
    this.notificationSettingsFormGroup.get('enabled').patchValue(!this.notificationSettingsFormGroup.get('enabled').value,
      {emitEvent: true});
  }

  getChecked(deliveryMethod: NotificationDeliveryMethod): boolean {
    return this.notificationSettingsFormGroup.get('enabledDeliveryMethods').value.includes(deliveryMethod);
  }

  toggleDeliviryMethod(deliveryMethod: NotificationDeliveryMethod) {
    const enabledDeliveryMethods = this.notificationSettingsFormGroup.get('enabledDeliveryMethods').value;
    if (enabledDeliveryMethods.includes(deliveryMethod)) {
      enabledDeliveryMethods.splice(enabledDeliveryMethods.indexOf(deliveryMethod), 1);
    } else {
      enabledDeliveryMethods.push(deliveryMethod);
    }
    this.notificationSettingsFormGroup.get('enabledDeliveryMethods').patchValue(enabledDeliveryMethods);
  }

  writeValue(value: NotificationUserSetting): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (isDefinedAndNotNull(this.modelValue)) {
      this.notificationSettingsFormGroup.patchValue(this.modelValue, {emitEvent: false});
      if (!this.disabled && !this.notificationSettingsFormGroup.valid) {
        this.updateModel();
      }
    }
  }

  private updateModel() {
    const value = this.notificationSettingsFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}
