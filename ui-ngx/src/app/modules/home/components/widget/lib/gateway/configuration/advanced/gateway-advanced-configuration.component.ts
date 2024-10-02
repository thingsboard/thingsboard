///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, forwardRef, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { GatewayConfigValue } from '@home/components/widget/lib/gateway/configuration/models/gateway-configuration.models';

@Component({
  selector: 'tb-gateway-advanced-configuration',
  templateUrl: './gateway-advanced-configuration.component.html',
  styleUrls: ['./gateway-advanced-configuration.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GatewayAdvancedConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GatewayAdvancedConfigurationComponent),
      multi: true
    }
  ],
})
export class GatewayAdvancedConfigurationComponent implements OnDestroy, ControlValueAccessor, Validators {

  advancedFormControl: FormControl;

  private onChange: (value: unknown) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.advancedFormControl = this.fb.control('');
    this.advancedFormControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(advancedConfig: GatewayConfigValue): void {
    this.advancedFormControl.reset(advancedConfig, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.advancedFormControl.valid ? null : {
      advancedFormControl: {valid: false}
    };
  }
}
