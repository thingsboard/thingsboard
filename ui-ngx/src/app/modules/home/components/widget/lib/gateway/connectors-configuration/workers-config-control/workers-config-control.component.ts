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
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

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
    }
  ]
})
export class WorkersConfigControlComponent implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() controlKey = 'workers';

  workersConfigFormGroup: UntypedFormGroup;

  get parentFormGroup(): FormGroup {
    return this.parentContainer.control as FormGroup;
  }

  private parentContainer = inject(ControlContainer);

  constructor(private fb: FormBuilder) {
    this.workersConfigFormGroup = this.fb.group({
      maxNumberOfWorkers: [100, [Validators.required, Validators.min(1)]],
      maxMessageNumberPerWorker: [10, [Validators.required, Validators.min(1)]],
    });
  }

  ngOnInit(): void {
    this.addSelfControl();
  }

  ngOnDestroy(): void {
    this.removeSelfControl();
  }

  registerOnChange(fn: any): void {}

  registerOnTouched(fn: any): void {}

  writeValue(obj: any): void {}

  private addSelfControl(): void {
    this.parentFormGroup.addControl(this.controlKey,  this.workersConfigFormGroup);
  }

  private removeSelfControl(): void {
    this.parentFormGroup.removeControl(this.controlKey);
  }
}
