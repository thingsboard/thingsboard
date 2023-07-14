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

import { Component, Inject, InjectionToken, OnInit } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  TimeseriesDeleteStrategy,
  timeseriesDeleteStrategyTranslations
} from '@shared/models/telemetry/telemetry.models';
import { MINUTE } from '@shared/models/time/time.models';

export const DELETE_TIMESERIES_PANEL_DATA = new InjectionToken<any>('DeleteTimeseriesPanelData');

export interface DeleteTimeseriesPanelData {
  isMultipleDeletion: boolean;
}

@Component({
  selector: 'tb-delete-timeseries-panel',
  templateUrl: './delete-timeseries-panel.component.html',
  styleUrls: ['./delete-timeseries-panel.component.scss']
})
export class DeleteTimeseriesPanelComponent implements OnInit {

  strategy: string = TimeseriesDeleteStrategy.DELETE_ALL_DATA_INCLUDING_KEY;

  result: string = null;

  startDateTime: Date;

  endDateTime: Date;

  rewriteLatestIfDeleted: boolean = true;

  strategiesTranslationsMap = timeseriesDeleteStrategyTranslations;

  multipleDeletionStrategies = [
    TimeseriesDeleteStrategy.DELETE_ALL_DATA_INCLUDING_KEY,
    TimeseriesDeleteStrategy.DELETE_OLD_DATA_EXCEPT_LATEST_VALUE
  ];

  constructor(@Inject(DELETE_TIMESERIES_PANEL_DATA) public data: DeleteTimeseriesPanelData,
              public overlayRef: OverlayRef) { }

  ngOnInit(): void {
    let today = new Date();
    this.startDateTime = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
    this.endDateTime = today;
    if (this.data.isMultipleDeletion) {
      this.strategiesTranslationsMap = new Map(Array.from(this.strategiesTranslationsMap.entries())
        .filter(([strategy]) => {
          return this.multipleDeletionStrategies.includes(strategy);
      }))
    }
  }

  delete(): void {
    this.result = this.strategy;
    this.overlayRef.dispose();
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  isPeriodStrategy(): boolean {
    return this.strategy === TimeseriesDeleteStrategy.DELETE_DATA_FOR_TIME_PERIOD;
  }

  isDeleteLatestStrategy(): boolean {
    return this.strategy === TimeseriesDeleteStrategy.DELETE_LATEST_VALUE;
  }

  onStartDateTimeChange(newStartDateTime: Date) {
    const endDateTimeTs = this.endDateTime.getTime();
    if (newStartDateTime.getTime() >= endDateTimeTs) {
      this.startDateTime = new Date(endDateTimeTs - MINUTE);
    } else {
      this.startDateTime = newStartDateTime;
    }
  }

  onEndDateTimeChange(newEndDateTime: Date) {
    const startDateTimeTs = this.startDateTime.getTime();
    if (newEndDateTime.getTime() <= startDateTimeTs) {
      this.endDateTime = new Date(startDateTimeTs + MINUTE);
    } else {
      this.endDateTime = newEndDateTime;
    }
  }
}
