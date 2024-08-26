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
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy
} from '@angular/core';
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
import {
  ModbusSecurity,
  noLeadTrailSpacesRegex,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-modbus-security-config',
  templateUrl: './modbus-security-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusSecurityConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusSecurityConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ]
})
export class ModbusSecurityConfigComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @coerceBoolean()
  @Input() isMaster = false;

  securityConfigFormGroup: UntypedFormGroup;

  private disabled = false;

  private onChange: (value: ModbusSecurity) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.securityConfigFormGroup = this.fb.group({
      certfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      keyfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      server_hostname: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      reqclicert: [{value: false, disabled: true}],
    });

    this.observeValueChanges();
  }

  ngOnChanges(): void {
    this.updateMasterEnabling();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: ModbusSecurity) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.securityConfigFormGroup.disable({emitEvent: false});
    } else {
      this.securityConfigFormGroup.enable({emitEvent: false});
    }
    this.updateMasterEnabling();
    this.cdr.markForCheck();
  }

  validate(): ValidationErrors | null {
    return this.securityConfigFormGroup.valid ? null : {
      securityConfigFormGroup: { valid: false }
    };
  }

  writeValue(securityConfig: ModbusSecurity): void {
    const { certfile, password, keyfile, server_hostname } = securityConfig;
    const securityState = {
      certfile: certfile ?? '',
      password: password ?? '',
      keyfile: keyfile ?? '',
      server_hostname: server_hostname ?? '',
      reqclicert: !!securityConfig.reqclicert,
    };

    this.securityConfigFormGroup.reset(securityState, {emitEvent: false});
  }

  private updateMasterEnabling(): void {
    if (this.isMaster) {
      if (!this.disabled) {
        this.securityConfigFormGroup.get('reqclicert').enable({emitEvent: false});
      }
      this.securityConfigFormGroup.get('server_hostname').disable({emitEvent: false});
    } else {
      if (!this.disabled) {
        this.securityConfigFormGroup.get('server_hostname').enable({emitEvent: false});
      }
      this.securityConfigFormGroup.get('reqclicert').disable({emitEvent: false});
    }
  }

  private observeValueChanges(): void {
    this.securityConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: ModbusSecurity) => {
      this.onChange(value);
      this.onTouched();
    });
  }
}
