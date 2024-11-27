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
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {
  SecurityType,
  SecurityTypeTranslationsMap,
  ModeType,
  noLeadTrailSpacesRegex,
  ConnectorSecurity
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-security-config',
  templateUrl: './security-config.component.html',
  styleUrls: ['./security-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SecurityConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports:[
    CommonModule,
    SharedModule,
  ]
})
export class SecurityConfigComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  title = 'gateway.security';

  @Input()
  @coerceBoolean()
  extendCertificatesModel = false;

  BrokerSecurityType = SecurityType;
  securityTypes = Object.values(SecurityType) as SecurityType[];
  modeTypes = Object.values(ModeType);
  SecurityTypeTranslationsMap = SecurityTypeTranslationsMap;
  securityFormGroup: UntypedFormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.securityFormGroup = this.fb.group({
      type: [SecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToCACert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToPrivateKey: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToClientCert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    if (this.extendCertificatesModel) {
      this.securityFormGroup.addControl('mode', this.fb.control(ModeType.NONE, []));
    }
    this.securityFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
    this.securityFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => this.updateValidators(type));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(securityInfo: ConnectorSecurity): void {
    if (!securityInfo) {
      const defaultSecurity = {type: SecurityType.ANONYMOUS};
      this.securityFormGroup.reset(defaultSecurity, {emitEvent: false});
    } else {
      if (!securityInfo.type) {
        securityInfo.type = SecurityType.ANONYMOUS;
      }
      this.updateValidators(securityInfo.type);
      this.securityFormGroup.reset(securityInfo, {emitEvent: false});
    }
    this.cdr.markForCheck();
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.get('type').value !== SecurityType.BASIC || this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  private updateValidators(type: SecurityType): void {
    if (type) {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
      this.securityFormGroup.get('pathToCACert').disable({emitEvent: false});
      this.securityFormGroup.get('pathToPrivateKey').disable({emitEvent: false});
      this.securityFormGroup.get('pathToClientCert').disable({emitEvent: false});
      this.securityFormGroup.get('mode')?.disable({emitEvent: false});
      if (type === SecurityType.BASIC) {
        this.securityFormGroup.get('username').enable({emitEvent: false});
        this.securityFormGroup.get('password').enable({emitEvent: false});
      } else if (type === SecurityType.CERTIFICATES) {
        this.securityFormGroup.get('pathToCACert').enable({emitEvent: false});
        this.securityFormGroup.get('pathToPrivateKey').enable({emitEvent: false});
        this.securityFormGroup.get('pathToClientCert').enable({emitEvent: false});
        if (this.extendCertificatesModel) {
          const modeControl = this.securityFormGroup.get('mode');
          if (modeControl && !modeControl.value) {
            modeControl.setValue(ModeType.NONE, {emitEvent: false});
          }

          modeControl?.enable({emitEvent: false});
          this.securityFormGroup.get('username').enable({emitEvent: false});
          this.securityFormGroup.get('password').enable({emitEvent: false});
        }
      }
    }
  }
}
