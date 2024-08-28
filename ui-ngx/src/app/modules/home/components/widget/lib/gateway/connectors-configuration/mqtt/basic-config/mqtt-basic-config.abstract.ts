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

import { ControlValueAccessor, FormBuilder, FormGroup, ValidationErrors, Validator } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isObject } from 'lodash';
import {
  MappingType,
  MQTTBasicConfig,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestMappingValue,
  RequestType
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Directive, OnDestroy } from '@angular/core';

@Directive()
export abstract class AbstractMqttBasicConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  basicFormGroup: FormGroup;
  mappingTypes = MappingType;
  private destroy$ = new Subject<void>();
  private onChange: (value: MQTTBasicConfig | MQTTLegacyBasicConfig) => void;
  private onTouched: () => void;

  constructor(protected fb: FormBuilder) {
    this.basicFormGroup = this.fb.group({
      mapping: [],
      requestsMapping: [],
      broker: [],
      workers: [],
    });

    this.basicFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(this.getMappedMQTTConfig(value));
        this.onTouched();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: MQTTBasicConfig | MQTTLegacyBasicConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.basicFormGroup.valid ? null : {
      basicFormGroup: {valid: false}
    };
  }

  protected getRequestDataArray(value: Record<RequestType, RequestMappingData[]>): RequestMappingData[] {
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

  protected getRequestDataObject(array: RequestMappingValue[]): Record<RequestType, RequestMappingValue[]> {
    return array.reduce((result, { requestType, requestValue }) => {
      result[requestType].push(requestValue);
      return result;
    }, {
      connectRequests: [],
      disconnectRequests: [],
      attributeRequests: [],
      attributeUpdates: [],
      serverSideRpc: [],
    });
  }

  abstract writeValue(basicConfig: MQTTBasicConfig | MQTTLegacyBasicConfig): void;
  protected abstract getMappedMQTTConfig(basicConfig: MQTTBasicConfig): MQTTBasicConfig | MQTTLegacyBasicConfig;
}
