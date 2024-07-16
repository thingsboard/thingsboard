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

import { ChangeDetectionStrategy, Component, forwardRef, Input, OnDestroy, TemplateRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ConnectorBaseConfig,
  MappingType,
  RequestMappingData,
  RequestType,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import {
  BrokerConfigControlComponent,
  MappingTableComponent,
  SecurityConfigComponent,
  WorkersConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { isObject } from 'lodash';

@Component({
  selector: 'tb-mqtt-basic-config',
  templateUrl: './mqtt-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    WorkersConfigControlComponent,
    BrokerConfigControlComponent,
    MappingTableComponent,
  ],
  styleUrls: ['./mqtt-basic-config.component.scss']
})

export class MqttBasicConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  @Input()
  generalTabContent: TemplateRef<any>;

  mappingTypes = MappingType;
  basicFormGroup: FormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.basicFormGroup = this.fb.group({
      dataMapping: [],
      requestsMapping: [],
      broker: [],
      workers: [],
    });

    this.basicFormGroup.valueChanges
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

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(basicConfig: ConnectorBaseConfig): void {
    const editedBase = {
      workers: {
        maxNumberOfWorkers: basicConfig.broker?.maxNumberOfWorkers,
        maxMessageNumberPerWorker: basicConfig.broker?.maxMessageNumberPerWorker,
      },
      dataMapping: basicConfig.dataMapping || [],
      broker: basicConfig.broker || {},
      requestsMapping: Array.isArray(basicConfig.requestsMapping)
        ? basicConfig.requestsMapping
        : this.getRequestDataArray(basicConfig.requestsMapping),
    };

    this.basicFormGroup.setValue(editedBase, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.basicFormGroup.valid ? null : {
      basicFormGroup: {valid: false}
    };
  }

  private getRequestDataArray(value: Record<RequestType, RequestMappingData>): RequestMappingData[] {
    const mappingConfigs = [];

    if (isObject(value)) {
      Object.keys(value).forEach((configKey: string) => {
        for (const mapping of value[configKey]) {
          mappingConfigs.push({
            requestType: configKey,
            requestValue: mapping
          });
        }
      });
    }

    return mappingConfigs;
  }
}
