///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Inject, InjectionToken, OnDestroy, OnInit } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  TimeseriesDeleteStrategy,
  timeseriesDeleteStrategyTranslations
} from '@shared/models/telemetry/telemetry.models';
import { MINUTE } from '@shared/models/time/time.models';
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';

export const DELETE_TIMESERIES_PANEL_DATA = new InjectionToken<any>('DeleteTimeseriesPanelData');

export interface DeleteTimeseriesPanelData {
  isMultipleDeletion: boolean;
}

@Component({
  selector: 'tb-delete-timeseries-panel',
  templateUrl: './delete-timeseries-panel.component.html',
  styleUrls: ['./delete-timeseries-panel.component.scss']
})
export class DeleteTimeseriesPanelComponent implements OnInit, OnDestroy {

  deleteTimeseriesFormGroup: UntypedFormGroup;

  startDateTimeSubscription: Subscription;

  endDateTimeSubscription: Subscription;

  result: string = null;

  strategiesTranslationsMap = timeseriesDeleteStrategyTranslations;

  multipleDeletionStrategies = [
    TimeseriesDeleteStrategy.DELETE_ALL_DATA,
    TimeseriesDeleteStrategy.DELETE_ALL_DATA_EXCEPT_LATEST_VALUE
  ];

  constructor(@Inject(DELETE_TIMESERIES_PANEL_DATA) public data: DeleteTimeseriesPanelData,
              public overlayRef: OverlayRef,
              public fb: UntypedFormBuilder) { }

  ngOnInit(): void {
    const today = new Date();
    if (this.data.isMultipleDeletion) {
      this.strategiesTranslationsMap = new Map(Array.from(this.strategiesTranslationsMap.entries())
        .filter(([strategy]) => {
          return this.multipleDeletionStrategies.includes(strategy);
      }))
    }
    this.deleteTimeseriesFormGroup = this.fb.group({
      strategy: [TimeseriesDeleteStrategy.DELETE_ALL_DATA],
      startDateTime: [new Date(today.getFullYear(), today.getMonth() - 1, today.getDate())],
      endDateTime: [today],
      rewriteLatest: [true]
    })
    this.startDateTimeSubscription = this.getStartDateTimeFormControl().valueChanges.subscribe(
      value => this.onStartDateTimeChange(value)
    )
    this.endDateTimeSubscription = this.getEndDateTimeFormControl().valueChanges.subscribe(
      value => this.onEndDateTimeChange(value)
    )
  }

  ngOnDestroy(): void {
    this.startDateTimeSubscription.unsubscribe();
    this.startDateTimeSubscription = null;
    this.endDateTimeSubscription.unsubscribe();
    this.endDateTimeSubscription = null;
  }

  delete(): void {
    this.result = this.getStrategyFormControl().value;
    this.overlayRef.dispose();
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  isPeriodStrategy(): boolean {
    return this.getStrategyFormControl().value === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD;
  }

  isDeleteLatestStrategy(): boolean {
    return this.getStrategyFormControl().value === TimeseriesDeleteStrategy.DELETE_LATEST_VALUE;
  }

  getStrategyFormControl(): AbstractControl {
    return this.deleteTimeseriesFormGroup.get('strategy');
  }

  getStartDateTimeFormControl(): AbstractControl {
    return this.deleteTimeseriesFormGroup.get('startDateTime');
  }

  getEndDateTimeFormControl(): AbstractControl {
    return this.deleteTimeseriesFormGroup.get('endDateTime');
  }

  getRewriteLatestFormControl(): AbstractControl {
    return this.deleteTimeseriesFormGroup.get('rewriteLatest');
  }

  onStartDateTimeChange(newStartDateTime: Date) {
    if (newStartDateTime) {
      const endDateTimeTs = this.deleteTimeseriesFormGroup.get('endDateTime').value.getTime();
      const startDateTimeControl = this.getStartDateTimeFormControl();
      if (newStartDateTime.getTime() >= endDateTimeTs) {
        startDateTimeControl.patchValue(new Date(endDateTimeTs - MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        startDateTimeControl.patchValue(newStartDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }

  onEndDateTimeChange(newEndDateTime: Date) {
    if (newEndDateTime) {
      const startDateTimeTs = this.deleteTimeseriesFormGroup.get('startDateTime').value.getTime();
      const endDateTimeControl = this.getEndDateTimeFormControl();
      if (newEndDateTime.getTime() <= startDateTimeTs) {
        endDateTimeControl.patchValue(new Date(startDateTimeTs + MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        endDateTimeControl.patchValue(newEndDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }
}
