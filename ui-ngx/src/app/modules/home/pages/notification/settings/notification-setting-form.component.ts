// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';
import {
  NotificationDeliveryMethod,
  NotificationTemplateTypeTranslateMap,
  NotificationUserSetting
} from '@shared/models/notification.models';

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
    ],
    standalone: false
})
export class NotificationSettingFormComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  deliveryMethods: NotificationDeliveryMethod[] = [];

  notificationSettingsFormGroup: UntypedFormGroup;

  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  private propagateChange = null;

  private valueChange$: Subscription = null;

  constructor(private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const deliveryMethod = {};
    this.deliveryMethods.forEach(value => {
      deliveryMethod[value] = true;
    });
    this.notificationSettingsFormGroup = this.fb.group(
      {
        name: [''],
        enabled: [true],
        enabledDeliveryMethods: this.fb.group({
          ...deliveryMethod
        })
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

  getChecked(deliveryMethod?: NotificationDeliveryMethod): boolean {
    if (deliveryMethod) {
      return this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod).value;
    } else {
      const enabledDeliveryMethod = Object.values(this.notificationSettingsFormGroup.get('enabledDeliveryMethods').value);
      const checked = enabledDeliveryMethod.some(value => value === true);
      if (checked !== this.notificationSettingsFormGroup.get('enabled').value) {
        setTimeout(() => this.notificationSettingsFormGroup.get('enabled').patchValue(checked), 0);
      }
      return enabledDeliveryMethod.every(value => value);
    }
  }

  toggleDeliviryMethod(deliveryMethod: NotificationDeliveryMethod) {
    this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod)
      .patchValue(!this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod).value);
  }

  changeInstanceTypeCheckBox(value: any) {
    const enabledDeliveryMethod = deepClone(this.notificationSettingsFormGroup.get('enabledDeliveryMethods').value);
    Object.keys(enabledDeliveryMethod).forEach(key => {
      enabledDeliveryMethod[key] = value;
    });
    this.notificationSettingsFormGroup.get('enabled').patchValue(value, {emitEvent: false});
    this.notificationSettingsFormGroup.get('enabledDeliveryMethods').patchValue(enabledDeliveryMethod);
    this.notificationSettingsFormGroup.markAsDirty();
  }

  getIndeterminate() {
    if (!this.notificationSettingsFormGroup.get('enabled').value) {
      return false;
    }
    const enabledDeliveryMethod: Array<boolean> = Object.values(this.notificationSettingsFormGroup.get('enabledDeliveryMethods').value);
    const checkedResource = enabledDeliveryMethod.filter(value => value);
    return checkedResource.length !== 0 && checkedResource.length !== enabledDeliveryMethod.length;
  }

  writeValue(value: NotificationUserSetting): void {
    if (isDefinedAndNotNull(value)) {
      this.notificationSettingsFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
      this.propagateChange(this.notificationSettingsFormGroup.value);
  }
}
