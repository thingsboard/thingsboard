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

import { Component, InjectionToken, OnDestroy, OnInit } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  TimeseriesDeleteStrategy,
  timeseriesDeleteStrategyTranslations
} from '@shared/models/telemetry/telemetry.models';
import { MINUTE } from '@shared/models/time/time.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export const DELETE_TIMESERIES_PANEL_DATA = new InjectionToken<any>('DeleteTimeseriesPanelData');


export interface DeleteTimeseriesPanelResult {
  strategy: TimeseriesDeleteStrategy;
  startDateTime: Date;
  endDateTime: Date;
  rewriteLatest: boolean;
}

@Component({
    selector: 'tb-delete-timeseries-panel',
    templateUrl: './delete-timeseries-panel.component.html',
    styleUrls: ['./delete-timeseries-panel.component.scss'],
    standalone: false
})
export class DeleteTimeseriesPanelComponent implements OnInit, OnDestroy {

  deleteTimeseriesFormGroup: FormGroup;

  result: DeleteTimeseriesPanelResult = null;

  strategiesTranslationsMap = timeseriesDeleteStrategyTranslations;

  private destroy$ = new Subject<void>();

  constructor(private overlayRef: OverlayRef,
              private fb: FormBuilder) { }

  ngOnInit(): void {
    const today = new Date();
    this.deleteTimeseriesFormGroup = this.fb.group({
      strategy: [TimeseriesDeleteStrategy.DELETE_ALL_DATA],
      startDateTime: [
        { value: new Date(today.getFullYear(), today.getMonth() - 1, today.getDate()), disabled: true },
        Validators.required
      ],
      endDateTime: [{ value: today, disabled: true }, Validators.required],
      rewriteLatest: [true]
    })

    this.deleteTimeseriesFormGroup.get('strategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD) {
        this.deleteTimeseriesFormGroup.get('startDateTime').enable({onlySelf: true, emitEvent: false});
        this.deleteTimeseriesFormGroup.get('endDateTime').enable({onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('startDateTime').disable({onlySelf: true, emitEvent: false});
        this.deleteTimeseriesFormGroup.get('endDateTime').disable({onlySelf: true, emitEvent: false});
      }
    })
    this.deleteTimeseriesFormGroup.get('startDateTime').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.onStartDateTimeChange(value));
    this.deleteTimeseriesFormGroup.get('endDateTime').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.onEndDateTimeChange(value));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  delete(): void {
    if (this.deleteTimeseriesFormGroup.valid) {
      this.result = this.deleteTimeseriesFormGroup.value;
      this.overlayRef.dispose();
    } else {
      this.deleteTimeseriesFormGroup.markAllAsTouched();
    }
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  isPeriodStrategy(): boolean {
    return this.deleteTimeseriesFormGroup.get('strategy').value === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD;
  }

  isDeleteLatestStrategy(): boolean {
    return this.deleteTimeseriesFormGroup.get('strategy').value === TimeseriesDeleteStrategy.DELETE_LATEST_VALUE;
  }

  private onStartDateTimeChange(newStartDateTime: Date) {
    if (newStartDateTime) {
      const endDateTimeTs = this.deleteTimeseriesFormGroup.get('endDateTime').value.getTime();
      if (newStartDateTime.getTime() >= endDateTimeTs) {
        this.deleteTimeseriesFormGroup.get('startDateTime')
          .patchValue(new Date(endDateTimeTs - MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('startDateTime')
          .patchValue(newStartDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }

  private onEndDateTimeChange(newEndDateTime: Date) {
    if (newEndDateTime) {
      const startDateTimeTs = this.deleteTimeseriesFormGroup.get('startDateTime').value.getTime();
      if (newEndDateTime.getTime() <= startDateTimeTs) {
        this.deleteTimeseriesFormGroup.get('endDateTime')
          .patchValue(new Date(startDateTimeTs + MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('endDateTime')
          .patchValue(newEndDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }
}
