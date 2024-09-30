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
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator, Validators,
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import {
  integerRegex,
  noLeadTrailSpacesRegex,
  RPCTemplateConfigMQTT
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Component({
  selector: 'tb-mqtt-rpc-parameters',
  templateUrl: './mqtt-rpc-parameters.component.html',
  styleUrls: ['./mqtt-rpc-parameters.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttRpcParametersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttRpcParametersComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class MqttRpcParametersComponent implements ControlValueAccessor, Validator, OnDestroy {

  rpcParametersFormGroup: UntypedFormGroup;

  private onChange: (value: RPCTemplateConfigMQTT) => void = (_) => {};
  private onTouched: () => void = () => {};

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.rpcParametersFormGroup = this.fb.group({
      methodFilter: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      requestTopicExpression: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      responseTopicExpression: [{ value: null, disabled: true }, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      responseTimeout: [{ value: null, disabled: true }, [Validators.min(10), Validators.pattern(integerRegex)]],
      valueExpression: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      withResponse: [false, []],
    });

    this.observeValueChanges();
    this.observeWithResponse();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: RPCTemplateConfigMQTT) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.rpcParametersFormGroup.valid ? null : {
      rpcParametersFormGroup: { valid: false }
    };
  }

  writeValue(value: RPCTemplateConfigMQTT): void {
    this.rpcParametersFormGroup.patchValue(value, {emitEvent: false});
    this.toggleResponseFields(value.withResponse);
  }

  private observeValueChanges(): void {
    this.rpcParametersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
  }

  private observeWithResponse(): void {
    this.rpcParametersFormGroup.get('withResponse').valueChanges.pipe(
      tap((isActive: boolean) => this.toggleResponseFields(isActive)),
      takeUntil(this.destroy$),
    ).subscribe();
  }

  private toggleResponseFields(enabled: boolean): void {
    const responseTopicControl = this.rpcParametersFormGroup.get('responseTopicExpression');
    const responseTimeoutControl = this.rpcParametersFormGroup.get('responseTimeout');
    if (enabled) {
      responseTopicControl.enable();
      responseTimeoutControl.enable();
    } else {
      responseTopicControl.disable();
      responseTimeoutControl.disable();
    }
  }
}
