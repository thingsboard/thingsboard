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

import { ChangeDetectionStrategy, Component, forwardRef, OnDestroy } from '@angular/core';
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
  BrokerConfig,
  MqttVersions,
  noLeadTrailSpacesRegex,
  PortLimits,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { generateSecret } from '@core/utils';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-broker-config-control',
  templateUrl: './broker-config-control.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BrokerConfigControlComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => BrokerConfigControlComponent),
      multi: true
    }
  ]
})
export class BrokerConfigControlComponent implements ControlValueAccessor, Validator, OnDestroy {
  brokerConfigFormGroup: UntypedFormGroup;
  mqttVersions = MqttVersions;
  portLimits = PortLimits;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.brokerConfigFormGroup = this.fb.group({
      name: ['', []],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      version: [5, []],
      clientId: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      security: []
    });

    this.brokerConfigFormGroup.valueChanges.subscribe(value => {
      this.onChange(value);
      this.onTouched();
    });
  }

  get portErrorTooltip(): string {
    if (this.brokerConfigFormGroup.get('port').hasError('required')) {
      return this.translate.instant('gateway.port-required');
    } else if (
      this.brokerConfigFormGroup.get('port').hasError('min') ||
      this.brokerConfigFormGroup.get('port').hasError('max')
    ) {
      return this.translate.instant('gateway.port-limits-error',
        {min: PortLimits.MIN, max: PortLimits.MAX});
    }
    return '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  generate(formControlName: string): void {
    this.brokerConfigFormGroup.get(formControlName)?.patchValue('tb_gw_' + generateSecret(5));
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(brokerConfig: BrokerConfig): void {
    this.brokerConfigFormGroup.patchValue(brokerConfig, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.brokerConfigFormGroup.valid ? null : {
      brokerConfigFormGroup: {valid: false}
    };
  }
}
