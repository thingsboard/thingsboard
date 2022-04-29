///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { DeviceProfileAlarm } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { UtilsService } from '@core/services/utils.service';
import { QueueProcessingStrategyTypes, QueueSubmitStrategyTypes } from '@shared/models/queue.models';

@Component({
  selector: 'tb-tenant-profile-queue',
  templateUrl: './tenant-profile-queue.component.html',
  styleUrls: ['./tenant-profile-queue.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TenantProfileQueueComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TenantProfileQueueComponent),
      multi: true,
    }
  ]
})
export class TenantProfileQueueComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Output()
  removeQueue = new EventEmitter();

  @Input()
  expanded = false;

  @Input()
  mainQueue = false;

  @Input()
  newQueue = false;

  private modelValue: DeviceProfileAlarm;

  queueFormGroup: FormGroup;

  submitStrategies: string[] = [];
  processingStrategies: string[] = [];

  hideBatchSize = false;

  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private dialog: MatDialog,
              private utils: UtilsService,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.modelValue);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.submitStrategies = Object.values(QueueSubmitStrategyTypes);
    this.processingStrategies = Object.values(QueueProcessingStrategyTypes);
    this.queueFormGroup = this.fb.group(
      {
        name: ['', [Validators.required]],
        pollInterval: [25, [Validators.min(1), Validators.required]],
        partitions: [10, [Validators.min(1), Validators.required]],
        consumerPerPartition: [false, []],
        packProcessingTimeout: [2000, [Validators.min(1), Validators.required]],
        submitStrategy: this.fb.group({
          type: [null, [Validators.required]],
          batchSize: [0, [Validators.min(1), Validators.required]],
        }),
        processingStrategy: this.fb.group({
          type: [null, [Validators.required]],
          retries: [3, [Validators.min(0), Validators.required]],
          failurePercentage: [ 0, [Validators.min(0), Validators.required, Validators.max(100)]],
          pauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
          maxPauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
        }),
        topic: ['']
      });
    this.queueFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.queueFormGroup.get('name').valueChanges.subscribe((value) => this.queueFormGroup.patchValue({topic: `tb_rule_engine.${value}`}));
    this.queueFormGroup.get('submitStrategy').get('type').valueChanges.subscribe(() => {
      this.submitStrategyTypeChanged();
    });
    if (this.newQueue) {
      this.queueFormGroup.get('name').enable({emitEvent: false});
    } else {
      this.queueFormGroup.get('name').disable({emitEvent: false});
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.queueFormGroup.disable({emitEvent: false});
    } else {
      this.queueFormGroup.enable({emitEvent: false});
      this.queueFormGroup.get('name').disable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileAlarm): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (!this.modelValue.alarmType) {
      this.expanded = true;
    }
    this.queueFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    if (!this.disabled && !this.queueFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: FormControl) {
    if (c.parent) {
      const queueName = c.value.name;
      const profileQueues = [];
      c.parent.getRawValue().forEach((queue) => {
          profileQueues.push(queue.name);
        }
      );
      if (profileQueues.filter(profileQueue => profileQueue === queueName).length > 1) {
        this.queueFormGroup.get('name').setErrors({
          unique: true
        });
      }
    }
    return (this.queueFormGroup.valid) ? null : {
      queue: {
        valid: false,
      },
    };
  }

  get queueTitle(): string {
    const queueName = this.queueFormGroup.get('name').value;
    return this.utils.customTranslation(queueName, queueName);
  }

  private updateModel() {
    const value = this.queueFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }

  submitStrategyTypeChanged() {
    const form = this.queueFormGroup.get('submitStrategy') as FormGroup;
    const type: QueueSubmitStrategyTypes = form.get('type').value;
    const batchSizeField = form.get('batchSize');
    if (type === QueueSubmitStrategyTypes.BATCH) {
      batchSizeField.enable();
      batchSizeField.patchValue(1000);
      this.hideBatchSize = true;
    } else {
      batchSizeField.patchValue(null);
      batchSizeField.disable();
      this.hideBatchSize = false;
    }
  }
}
