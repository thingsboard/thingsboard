///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, DestroyRef, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { UtilsService } from '@core/services/utils.service';
import {
  QueueInfo,
  QueueProcessingStrategyTypes,
  QueueProcessingStrategyTypesMap,
  QueueSubmitStrategyTypes,
  QueueSubmitStrategyTypesMap
} from '@shared/models/queue.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-queue-form',
  templateUrl: './queue-form.component.html',
  styleUrls: ['./queue-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => QueueFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => QueueFormComponent),
      multi: true,
    }
  ]
})
export class QueueFormComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  newQueue = false;

  @Input()
  systemQueue = false;

  queueFormGroup: UntypedFormGroup;
  hideBatchSize = false;

  queueSubmitStrategyTypes = QueueSubmitStrategyTypes;
  queueProcessingStrategyTypes = QueueProcessingStrategyTypes;
  submitStrategies: string[] = Object.values(this.queueSubmitStrategyTypes);
  processingStrategies: string[] = Object.values(this.queueProcessingStrategyTypes);
  queueSubmitStrategyTypesMap = QueueSubmitStrategyTypesMap;
  queueProcessingStrategyTypesMap = QueueProcessingStrategyTypesMap;

  private modelValue: QueueInfo;
  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private utils: UtilsService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
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
    this.queueFormGroup = this.fb.group(
      {
        name: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_.\-]+$/)]],
        pollInterval: [25, [Validators.min(1), Validators.required]],
        partitions: [10, [Validators.min(1), Validators.required]],
        consumerPerPartition: [false, []],
        packProcessingTimeout: [2000, [Validators.min(1), Validators.required]],
        submitStrategy: this.fb.group({
          type: [null, [Validators.required]],
          batchSize: [null],
        }),
        processingStrategy: this.fb.group({
          type: [null, [Validators.required]],
          retries: [3, [Validators.min(0), Validators.required]],
          failurePercentage: [ 0, [Validators.min(0), Validators.required, Validators.max(100)]],
          pauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
          maxPauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
        }),
        topic: [''],
        additionalInfo: this.fb.group({
          description: [''],
          customProperties: [''],
          duplicateMsgToAllPartitions: [false]
        })
      });
    this.queueFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.queueFormGroup.get('name').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      this.queueFormGroup.patchValue({topic: `tb_rule_engine.${value}`});
    });
    this.queueFormGroup.get('submitStrategy').get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.submitStrategyTypeChanged();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.queueFormGroup.disable({emitEvent: false});
    } else {
      this.queueFormGroup.enable({emitEvent: false});
      if (this.newQueue) {
        this.queueFormGroup.get('name').enable({emitEvent: false});
      } else {
        this.queueFormGroup.get('name').disable({emitEvent: false});
      }
    }
  }

  writeValue(value: QueueInfo): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (isDefinedAndNotNull(this.modelValue)) {
      this.queueFormGroup.patchValue(this.modelValue, {emitEvent: false});
      this.queueFormGroup.get('additionalInfo').get('description')
        .patchValue(this.modelValue.additionalInfo?.description, {emitEvent: false});
      this.queueFormGroup.get('additionalInfo').get('customProperties')
        .patchValue(this.modelValue.additionalInfo?.customProperties, {emitEvent: false});
      this.queueFormGroup.get('additionalInfo').get('duplicateMsgToAllPartitions')
        .patchValue(this.modelValue.additionalInfo?.duplicateMsgToAllPartitions, {emitEvent: false});
      this.submitStrategyTypeChanged();
      if (!this.disabled && !this.queueFormGroup.valid) {
        this.updateModel();
      }
    }
  }

  public validate(c: UntypedFormControl) {
    if (c.parent && !this.systemQueue) {
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
    const form = this.queueFormGroup.get('submitStrategy') as UntypedFormGroup;
    const type: QueueSubmitStrategyTypes = form.get('type').value;
    const batchSizeField = form.get('batchSize');
    if (type === QueueSubmitStrategyTypes.BATCH) {
      batchSizeField.patchValue(batchSizeField.value ?? 1000, {emitEvent: false});
      batchSizeField.setValidators([Validators.min(1), Validators.required]);
      batchSizeField.updateValueAndValidity({emitEvent: false});
      this.hideBatchSize = true;
    } else {
      batchSizeField.patchValue(null, {emitEvent: false});
      batchSizeField.clearValidators();
      batchSizeField.updateValueAndValidity({emitEvent: false});
      this.hideBatchSize = false;
    }
  }
}
