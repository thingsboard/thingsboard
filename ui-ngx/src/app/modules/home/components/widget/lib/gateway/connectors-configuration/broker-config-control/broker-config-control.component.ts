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
  inject,
  Input,
  OnDestroy,
  OnInit
} from '@angular/core';
import {
  ControlContainer,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  MqttVersions,
  noLeadTrailSpacesRegex,
  PortLimits,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { generateSecret } from '@core/utils';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration/public-api';

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
    }
  ]
})
export class BrokerConfigControlComponent implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() controlKey = 'broker';

  brokerConfigFormGroup: UntypedFormGroup;
  mqttVersions = MqttVersions;
  portLimits = PortLimits;

  get parentFormGroup(): FormGroup {
    return this.parentContainer.control as FormGroup;
  }

  private parentContainer = inject(ControlContainer);
  private translate = inject(TranslateService);

  constructor(private fb: FormBuilder) {
    this.brokerConfigFormGroup = this.fb.group({
      name: ['', []],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      version: [5, []],
      clientId: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      maxNumberOfWorkers: [100, [Validators.required, Validators.min(1)]],
      maxMessageNumberPerWorker: [10, [Validators.required, Validators.min(1)]],
      security: [{}, [Validators.required]]
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

  ngOnInit(): void {
    this.addSelfControl();
  }

  ngOnDestroy(): void {
    this.removeSelfControl();
  }

  generate(formControlName: string): void {
    this.brokerConfigFormGroup.get(formControlName)?.patchValue('tb_gw_' + generateSecret(5));
  }

  registerOnChange(fn: any): void {}

  registerOnTouched(fn: any): void {}

  writeValue(obj: any): void {}

  private addSelfControl(): void {
    this.parentFormGroup.addControl(this.controlKey,  this.brokerConfigFormGroup);
  }

  private removeSelfControl(): void {
    this.parentFormGroup.removeControl(this.controlKey);
  }
}
