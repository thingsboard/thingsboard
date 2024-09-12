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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, OnDestroy } from '@angular/core';
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
import { generateSecret } from '@core/utils';
import { Subject } from 'rxjs';
import { GatewayPortTooltipPipe } from '@home/components/widget/lib/gateway/pipes/gateway-port-tooltip.pipe';
import { SecurityConfigComponent } from '../../security-config/security-config.component';

@Component({
  selector: 'tb-broker-config-control',
  templateUrl: './broker-config-control.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    GatewayPortTooltipPipe,
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
              private cdr: ChangeDetectorRef) {
    this.brokerConfigFormGroup = this.fb.group({
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      version: [5, []],
      clientId: ['tb_gw_' + generateSecret(5), [Validators.pattern(noLeadTrailSpacesRegex)]],
      security: []
    });

    this.brokerConfigFormGroup.valueChanges.subscribe(value => {
      this.onChange(value);
      this.onTouched();
    });
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
    const {
      version = 5,
      clientId = `tb_gw_${generateSecret(5)}`,
      security = {},
    } = brokerConfig;

    this.brokerConfigFormGroup.reset({ ...brokerConfig, version, clientId, security }, { emitEvent: false });
    this.cdr.markForCheck();
  }

  validate(): ValidationErrors | null {
    return this.brokerConfigFormGroup.valid ? null : {
      brokerConfigFormGroup: {valid: false}
    };
  }
}
