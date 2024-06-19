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

import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  OnDestroy,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import {
  noLeadTrailSpacesRegex,
  RestSecurityType,
  RestSecurityTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-rest-connector-security',
  templateUrl: './rest-connector-security.component.html',
  styleUrls: ['./rest-connector-security.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RestConnectorSecurityComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RestConnectorSecurityComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
  ]
})
export class RestConnectorSecurityComponent implements ControlValueAccessor, Validator, OnDestroy {
  BrokerSecurityType = RestSecurityType;
  securityTypes: RestSecurityType[] = Object.values(RestSecurityType);
  SecurityTypeTranslationsMap = RestSecurityTypeTranslationsMap;
  securityFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder) {
    this.securityFormGroup = this.fb.group({
      type: [RestSecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
    });
    this.observeSecurityForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any): void {
    if (!deviceInfo.type) {
      deviceInfo.type = RestSecurityType.ANONYMOUS;
    }
    this.securityFormGroup.reset(deviceInfo);
    this.updateView(deviceInfo);
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  private updateView(value: any): void {
    this.propagateChange(value);
  }

  private updateValidators(type: RestSecurityType): void {
    if (type === RestSecurityType.BASIC) {
      this.securityFormGroup.get('username').enable({emitEvent: false});
      this.securityFormGroup.get('password').enable({emitEvent: false});
    } else {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
    }
  }

  private observeSecurityForm(): void {
    this.securityFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => this.updateView(value));

    this.securityFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => this.updateValidators(type));
  }
}
