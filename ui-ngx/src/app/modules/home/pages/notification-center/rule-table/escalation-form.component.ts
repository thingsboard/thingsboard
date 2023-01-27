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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator, Validators
} from '@angular/forms';
import { UtilsService } from '@core/services/utils.service';
import { isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';
import { NonConfirmedNotificationEscalation } from '@shared/models/notification.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-escalation-form',
  templateUrl: './escalation-form.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EscalationFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EscalationFormComponent),
      multi: true,
    }
  ]
})
export class EscalationFormComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  @Input()
  disabled: boolean;

  @Input()
  systemEscalation = false;

  escalationFormGroup: FormGroup;

  entityType = EntityType;

  private modelValue: NonConfirmedNotificationEscalation;
  private propagateChange = null;
  private propagateChangePending = false;
  private valueChange$: Subscription = null;

  constructor(private utils: UtilsService,
              private fb: FormBuilder) {
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
    this.escalationFormGroup = this.fb.group(
      {
        delayInSec: [null],
        notificationTargetId: [null, Validators.required],
      });
    this.valueChange$ = this.escalationFormGroup.valueChanges.subscribe(() => {
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
      this.escalationFormGroup.disable({emitEvent: false});
    } else {
      this.escalationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: NonConfirmedNotificationEscalation): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (isDefinedAndNotNull(this.modelValue)) {
      this.escalationFormGroup.patchValue(this.modelValue, {emitEvent: false});
    }
    if (!this.disabled && !this.escalationFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: FormControl) {
    return (this.escalationFormGroup.valid) ? null : {
      escalation: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.escalationFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}
