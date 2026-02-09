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

import { Component, DestroyRef, forwardRef, OnInit } from '@angular/core';
import { AlarmStatus, alarmStatusTranslations, PageComponent } from '@shared/public-api';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-alarm-status-select',
    templateUrl: './alarm-status-select.component.html',
    styleUrls: ['./alarm-status-select.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlarmStatusSelectComponent),
            multi: true
        }],
    standalone: false
})

export class AlarmStatusSelectComponent extends PageComponent implements OnInit, ControlValueAccessor {

  public alarmStatusGroup: FormGroup;

  private propagateChange = null;

  readonly alarmStatus = AlarmStatus;
  readonly alarmStatusTranslations = alarmStatusTranslations;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.alarmStatusGroup = this.fb.group({
      alarmStatus: [null, []]
    });

    this.alarmStatusGroup.get('alarmStatus').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      this.propagateChange(value);
    });
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.alarmStatusGroup.disable({emitEvent: false});
    } else {
      this.alarmStatusGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: Array<AlarmStatus>): void {
    this.alarmStatusGroup.get('alarmStatus').patchValue(value, {emitEvent: false});
  }
}
