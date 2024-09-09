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
  Validator,
  Validators
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { WorkersConfig } from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-workers-config-control',
  templateUrl: './workers-config-control.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WorkersConfigControlComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => WorkersConfigControlComponent),
      multi: true
    }
  ]
})
export class WorkersConfigControlComponent implements OnDestroy, ControlValueAccessor, Validator {

  workersConfigFormGroup: UntypedFormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.workersConfigFormGroup = this.fb.group({
      maxNumberOfWorkers: [100, [Validators.required, Validators.min(1)]],
      maxMessageNumberPerWorker: [10, [Validators.required, Validators.min(1)]],
    });

    this.workersConfigFormGroup.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
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

  writeValue(workersConfig: WorkersConfig): void {
    const { maxNumberOfWorkers, maxMessageNumberPerWorker } = workersConfig;
    this.workersConfigFormGroup.reset({
      maxNumberOfWorkers: maxNumberOfWorkers || 100,
      maxMessageNumberPerWorker: maxMessageNumberPerWorker || 10,
    }, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.workersConfigFormGroup.valid ? null : {
      workersConfigFormGroup: {valid: false}
    };
  }
}
