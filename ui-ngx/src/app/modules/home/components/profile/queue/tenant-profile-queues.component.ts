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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Subject } from 'rxjs';
import { QueueInfo } from '@shared/models/queue.models';
import { UtilsService } from '@core/services/utils.service';
import { guid } from '@core/utils';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-tenant-profile-queues',
  templateUrl: './tenant-profile-queues.component.html',
  styleUrls: ['./tenant-profile-queues.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TenantProfileQueuesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TenantProfileQueuesComponent),
      multi: true,
    }
  ]
})
export class TenantProfileQueuesComponent implements ControlValueAccessor, Validator, OnDestroy, OnInit {

  tenantProfileQueuesFormGroup: UntypedFormGroup;
  newQueue = false;
  idMap = [];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.tenantProfileQueuesFormGroup = this.fb.group({
      queues: this.fb.array([])
    });

    this.tenantProfileQueuesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get queuesFormArray(): UntypedFormArray {
    return this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.newQueue = false;
    if (this.disabled) {
      this.tenantProfileQueuesFormGroup.disable({emitEvent: false});
    } else {
      this.tenantProfileQueuesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(queues: Array<QueueInfo> | null): void {
    if (queues?.length === this.queuesFormArray.length) {
      this.queuesFormArray.patchValue(queues, {emitEvent: false});
    } else {
      const queuesControls: Array<AbstractControl> = [];
      if (queues) {
        queues.forEach((queue, index) => {
          if (!queue.id) {
            if (!this.idMap[index]) {
              this.idMap.push(guid());
            }
            queue.id = this.idMap[index];
          }
          queuesControls.push(this.fb.control(queue, [Validators.required]));
        });
      }
      this.tenantProfileQueuesFormGroup.setControl('queues', this.fb.array(queuesControls), {emitEvent: false});
      if (this.disabled) {
        this.tenantProfileQueuesFormGroup.disable({emitEvent: false});
      } else {
        this.tenantProfileQueuesFormGroup.enable({emitEvent: false});
      }
    }
  }

  public trackByQueue(index: number, queueControl: AbstractControl) {
    if (queueControl) {
      return queueControl.value.id;
    }
    return null;
  }

  public removeQueue(index: number) {
    (this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray).removeAt(index);
    this.idMap.splice(index, 1);
  }

  public addQueue() {
    const queue = {
      id: guid(),
      consumerPerPartition: false,
      name: '',
      packProcessingTimeout: 2000,
      partitions: 10,
      pollInterval: 25,
      processingStrategy: {
        failurePercentage: 0,
        maxPauseBetweenRetries: 3,
        pauseBetweenRetries: 3,
        retries: 3,
        type: ''
      },
      submitStrategy: {
        batchSize: 0,
        type: ''
      },
      topic: '',
      additionalInfo: {
        description: '',
        customProperties: '',
        duplicateMsgToAllPartitions: false
      }
    };
    this.idMap.push(queue.id);
    this.newQueue = true;
    const queuesArray = this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray;
    queuesArray.push(this.fb.control(queue, []));
    this.tenantProfileQueuesFormGroup.updateValueAndValidity();
    if (!this.tenantProfileQueuesFormGroup.valid) {
      this.updateModel();
    }
  }

  getTitle(value): string {
    return this.utils.customTranslation(value, value);
  }

  public validate(c: AbstractControl): ValidationErrors | null {
    return this.tenantProfileQueuesFormGroup.valid ? null : {
      queues: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const queues: Array<QueueInfo> = this.tenantProfileQueuesFormGroup.get('queues').value;
    this.propagateChange(queues);
  }
}
