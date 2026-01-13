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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/public-api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface MessageType {
  name: string;
  value: string;
}

@Component({
  selector: 'tb-output-message-type-autocomplete',
  templateUrl: './output-message-type-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OutputMessageTypeAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OutputMessageTypeAutocompleteComponent),
      multi: true
    }
  ]
})

export class OutputMessageTypeAutocompleteComponent implements ControlValueAccessor, Validator {

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  set required(value) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  get required() {
    return this.requiredValue;
  }

  messageTypeFormGroup: FormGroup;

  messageTypes: MessageType[] = [
    {
      name: 'Post attributes',
      value: 'POST_ATTRIBUTES_REQUEST'
    },
    {
      name: 'Post telemetry',
      value: 'POST_TELEMETRY_REQUEST'
    },
    {
      name: 'Custom',
      value: ''
    },
  ];

  private modelValue: string | null;
  private requiredValue: boolean;
  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.messageTypeFormGroup = this.fb.group({
      messageTypeAlias: [null, [Validators.required]],
      messageType: [{value: null, disabled: true}, [Validators.maxLength(255)]]
    });
    this.messageTypeFormGroup.get('messageTypeAlias').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.updateMessageTypeValue(value));
    this.messageTypeFormGroup.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.updateView());
  }

  registerOnTouched(_fn: any): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  writeValue(value: string | null): void {
    this.modelValue = value;
    let findMessage = this.messageTypes.find(msgType => msgType.value === value);
    if (!findMessage) {
      findMessage = this.messageTypes.find(msgType => msgType.value === '');
    }
    this.messageTypeFormGroup.get('messageTypeAlias').patchValue(findMessage, {emitEvent: false});
    this.messageTypeFormGroup.get('messageType').patchValue(value, {emitEvent: false});
  }

  validate() {
    if (!this.messageTypeFormGroup.valid) {
      return {
        messageTypeInvalid: true
      };
    }
    return null;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.messageTypeFormGroup.disable({emitEvent: false});
    } else {
      this.messageTypeFormGroup.enable({emitEvent: false});
      if (this.messageTypeFormGroup.get('messageTypeAlias').value?.name !== 'Custom') {
        this.messageTypeFormGroup.get('messageType').disable({emitEvent: false});
      }
    }
  }

  private updateView() {
    const value = this.messageTypeFormGroup.getRawValue().messageType;
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private updateValidators() {
    this.messageTypeFormGroup.get('messageType').setValidators(
        this.required ? [Validators.required, Validators.maxLength(255)] : [Validators.maxLength(255)]
    );
    this.messageTypeFormGroup.get('messageType').updateValueAndValidity({emitEvent: false});
  }

  private updateMessageTypeValue(choseMessageType: MessageType) {
    if (choseMessageType?.name !== 'Custom') {
      this.messageTypeFormGroup.get('messageType').disable({emitEvent: false});
    } else {
      this.messageTypeFormGroup.get('messageType').enable({emitEvent: false});
    }
    this.messageTypeFormGroup.get('messageType').patchValue(choseMessageType.value ?? null);
  }

}
