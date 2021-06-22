///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { SnmpCommunicationConfig, SnmpSpecType, SnmpSpecTypeTranslationMap } from '@shared/models/device.models';
import { Subject, Subscription } from 'rxjs';
import { isUndefinedOrNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-snmp-device-profile-communication-config',
  templateUrl: './snmp-device-profile-communication-config.component.html',
  styleUrls: ['./snmp-device-profile-communication-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SnmpDeviceProfileCommunicationConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SnmpDeviceProfileCommunicationConfigComponent),
      multi: true
    }]
})
export class SnmpDeviceProfileCommunicationConfigComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  snmpSpecTypes = Object.values(SnmpSpecType);
  snmpSpecTypeTranslationMap = SnmpSpecTypeTranslationMap;

  deviceProfileCommunicationConfig: FormGroup;

  @Input()
  disabled: boolean;

  private usedSpecType: SnmpSpecType[] = [];
  private valueChange$: Subscription = null;
  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
    this.deviceProfileCommunicationConfig = this.fb.group({
      communicationConfig: this.fb.array([])
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  communicationConfigFormArray(): FormArray {
    return this.deviceProfileCommunicationConfig.get('communicationConfig') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileCommunicationConfig.disable({emitEvent: false});
    } else {
      this.deviceProfileCommunicationConfig.enable({emitEvent: false});
    }
  }

  writeValue(communicationConfig: SnmpCommunicationConfig[]) {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
    const communicationConfigControl: Array<AbstractControl> = [];
    if (communicationConfig) {
      communicationConfig.forEach((config) => {
        communicationConfigControl.push(this.createdFormGroup(config));
      });
    }
    this.deviceProfileCommunicationConfig.setControl('communicationConfig', this.fb.array(communicationConfigControl));
    if (!communicationConfig || !communicationConfig.length) {
      this.addCommunicationConfig();
    }
    if (this.disabled) {
      this.deviceProfileCommunicationConfig.disable({emitEvent: false});
    } else {
      this.deviceProfileCommunicationConfig.enable({emitEvent: false});
    }
    this.valueChange$ = this.deviceProfileCommunicationConfig.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.updateUsedSpecType();
    if (!this.disabled && !this.deviceProfileCommunicationConfig.valid) {
      this.updateModel();
    }
  }

  public validate() {
    return this.deviceProfileCommunicationConfig.valid && this.deviceProfileCommunicationConfig.value.communicationConfig.length ? null : {
      communicationConfig: false
    };
  }

  public removeCommunicationConfig(index: number) {
    this.communicationConfigFormArray().removeAt(index);
  }


  get isAddEnabled(): boolean {
    return this.communicationConfigFormArray().length !== Object.keys(SnmpSpecType).length;
  }

  public addCommunicationConfig() {
    this.communicationConfigFormArray().push(this.createdFormGroup());
    this.deviceProfileCommunicationConfig.updateValueAndValidity();
    if (!this.deviceProfileCommunicationConfig.valid) {
      this.updateModel();
    }
  }

  private getFirstUnusedSeverity(): SnmpSpecType {
    for (const type of Object.values(SnmpSpecType)) {
      if (this.usedSpecType.indexOf(type) === -1) {
        return type;
      }
    }
    return null;
  }

  public isDisabledSeverity(type: SnmpSpecType, index: number): boolean {
    const usedIndex = this.usedSpecType.indexOf(type);
    return usedIndex > -1 && usedIndex !== index;
  }

  public isShowFrequency(type: SnmpSpecType): boolean {
    return type === SnmpSpecType.TELEMETRY_QUERYING || type === SnmpSpecType.CLIENT_ATTRIBUTES_QUERYING;
  }

  private updateUsedSpecType() {
    this.usedSpecType = [];
    const value: SnmpCommunicationConfig[] = this.deviceProfileCommunicationConfig.get('communicationConfig').value;
    value.forEach((rule, index) => {
      this.usedSpecType[index] = rule.spec;
    });
  }

  private createdFormGroup(value?: SnmpCommunicationConfig): FormGroup {
    if (isUndefinedOrNull(value)) {
      value = {
        spec: this.getFirstUnusedSeverity(),
        queryingFrequencyMs: 5000,
        mappings: null
      };
    }
    const form = this.fb.group({
      spec: [value.spec, Validators.required],
      mappings: [value.mappings]
    });
    if (this.isShowFrequency(value.spec)) {
      form.addControl('queryingFrequencyMs',
        this.fb.control(value.queryingFrequencyMs, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]));
    }
    form.get('spec').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(spec => {
      if (this.isShowFrequency(spec)) {
        form.addControl('queryingFrequencyMs',
          this.fb.control(5000, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]));
      } else {
        form.removeControl('queryingFrequencyMs');
      }
    });
    return form;
  }

  private updateModel() {
    const value: SnmpCommunicationConfig[] = this.deviceProfileCommunicationConfig.get('communicationConfig').value;
    value.forEach(config => {
      if (!this.isShowFrequency(config.spec)) {
        delete config.queryingFrequencyMs;
      }
    });
    this.updateUsedSpecType();
    this.propagateChange(value);
  }

}
