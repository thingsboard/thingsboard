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

import { Component, Inject, OnInit } from '@angular/core';
import {
  aggregationTranslations,
  AggregationType,
  DAY,
  HistoryWindowType,
  quickTimeIntervalPeriod,
  RealtimeWindowType,
  Timewindow,
  TimewindowType
} from '@shared/models/time/time.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { isDefined, isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { TranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface TimewindowConfigDialogData {
  quickIntervalOnly: boolean;
  aggregation: boolean;
  timewindow: Timewindow;
}

@Component({
  selector: 'tb-timewindow-config-dialog',
  templateUrl: './timewindow-config-dialog.component.html',
  styleUrls: ['./timewindow-config-dialog.component.scss']
})
export class TimewindowConfigDialogComponent extends PageComponent implements OnInit {

  quickIntervalOnly = false;

  aggregation = false;

  timewindow: Timewindow;

  timewindowForm: FormGroup;

  historyTypes = HistoryWindowType;

  realtimeTypes = RealtimeWindowType;

  timewindowTypes = TimewindowType;

  aggregationTypes = AggregationType;

  aggregations = Object.keys(AggregationType);

  aggregationTypesTranslations = aggregationTranslations;

  result: Timewindow;

  realtimeTimewindowOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant('timewindow.relative'),
      value: this.realtimeTypes.INTERVAL
    }
  ];

  historyTimewindowOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant('timewindow.last'),
      value: this.historyTypes.LAST_INTERVAL
    },
    {
      name: this.translate.instant('timewindow.fixed'),
      value: this.historyTypes.FIXED
    },
    {
      name: this.translate.instant('timewindow.relative'),
      value: this.historyTypes.INTERVAL
    }
  ];

  realtimeTypeSelectionAvailable: boolean;

  constructor(@Inject(MAT_DIALOG_DATA) public data: TimewindowConfigDialogData,
              public dialogRef: MatDialogRef<TimewindowConfigDialogComponent, Timewindow>,
              protected store: Store<AppState>,
              public fb: FormBuilder,
              private timeService: TimeService,
              private translate: TranslateService) {
    super(store);
    this.quickIntervalOnly = data.quickIntervalOnly;
    this.aggregation = data.aggregation;
    this.timewindow = data.timewindow;

    if (!this.quickIntervalOnly) {
      this.realtimeTimewindowOptions.unshift({
        name: this.translate.instant('timewindow.last'),
        value: this.realtimeTypes.LAST_INTERVAL
      });
    }

    this.realtimeTypeSelectionAvailable = this.realtimeTimewindowOptions.length > 1;
  }

  ngOnInit(): void {
    const realtime = this.timewindow.realtime;
    const history = this.timewindow.history;
    const aggregation = this.timewindow.aggregation;

    this.timewindowForm = this.fb.group({
      realtime: this.fb.group({
        realtimeType: [ isDefined(realtime?.realtimeType) ? this.timewindow.realtime.realtimeType : RealtimeWindowType.LAST_INTERVAL ],
        timewindowMs: [ isDefined(realtime?.timewindowMs) ? this.timewindow.realtime.timewindowMs : null ],
        interval: [ isDefined(realtime?.interval) ? this.timewindow.realtime.interval : null ],
        quickInterval: [ isDefined(realtime?.quickInterval) ? this.timewindow.realtime.quickInterval : null ],
        disableCustomInterval: [ isDefinedAndNotNull(this.timewindow.realtime?.disableCustomInterval)
          ? this.timewindow.realtime?.disableCustomInterval : false ],
        disableCustomGroupInterval: [ isDefinedAndNotNull(this.timewindow.realtime?.disableCustomGroupInterval)
          ? this.timewindow.realtime?.disableCustomGroupInterval : false ],
      }),
      history: this.fb.group({
        historyType: [ isDefined(history?.historyType) ? this.timewindow.history.historyType : HistoryWindowType.LAST_INTERVAL ],
        timewindowMs: [ isDefined(history?.timewindowMs) ? this.timewindow.history.timewindowMs : null ],
        interval: [ isDefined(history?.interval) ? this.timewindow.history.interval : null ],
        fixedTimewindow: [ isDefined(history?.fixedTimewindow) ? this.timewindow.history.fixedTimewindow : null ],
        quickInterval: [ isDefined(history?.quickInterval) ? this.timewindow.history.quickInterval : null ],
        disableCustomInterval: [ isDefinedAndNotNull(this.timewindow.history?.disableCustomInterval)
          ? this.timewindow.history?.disableCustomInterval : false ],
        disableCustomGroupInterval: [ isDefinedAndNotNull(this.timewindow.history?.disableCustomGroupInterval)
          ? this.timewindow.history?.disableCustomGroupInterval : false ],
      }),
      aggregation: this.fb.group({
        type: [ isDefined(aggregation?.type) ? this.timewindow.aggregation.type : null ],
        limit: [ isDefined(aggregation?.limit) ? this.checkLimit(this.timewindow.aggregation.limit) : null ]
      }),
      timezone: [ isDefined(this.timewindow.timezone) ? this.timewindow.timezone : null ],
      hideInterval: [ isDefinedAndNotNull(this.timewindow.hideInterval)
                      ? this.timewindow.hideInterval : false ],
      hideLastInterval: [{
        value: isDefinedAndNotNull(this.timewindow.hideLastInterval)
                      ? this.timewindow.hideLastInterval : false,
        disabled: this.timewindow.hideInterval
      }],
      hideQuickInterval: [{
        value: isDefinedAndNotNull(this.timewindow.hideQuickInterval)
                      ? this.timewindow.hideQuickInterval : false,
        disabled: this.timewindow.hideInterval
      }],
      hideAggregation: [ isDefinedAndNotNull(this.timewindow.hideAggregation)
                      ? this.timewindow.hideAggregation : false ],
      hideAggInterval: [ isDefinedAndNotNull(this.timewindow.hideAggInterval)
                      ? this.timewindow.hideAggInterval : false ],
      hideTimezone: [ isDefinedAndNotNull(this.timewindow.hideTimezone)
                      ? this.timewindow.hideTimezone : false ]
    });

    this.updateValidators(this.timewindowForm.get('aggregation.type').value);
    this.timewindowForm.get('aggregation.type').valueChanges.subscribe((aggregationType: AggregationType) => {
      this.updateValidators(aggregationType);
    });
    this.timewindowForm.get('hideInterval').valueChanges.subscribe((value: boolean) => {
      if (value) {
        this.timewindowForm.get('hideLastInterval').disable();
        this.timewindowForm.get('hideQuickInterval').disable();
      } else {
        this.timewindowForm.get('hideLastInterval').enable();
        this.timewindowForm.get('hideQuickInterval').enable();
      }
    });
  }

  private checkLimit(limit?: number): number {
    if (!limit || limit < this.minDatapointsLimit()) {
      return this.minDatapointsLimit();
    } else if (limit > this.maxDatapointsLimit()) {
      return this.maxDatapointsLimit();
    }
    return limit;
  }

  private updateValidators(aggType: AggregationType) {
    if (aggType !== AggregationType.NONE) {
      this.timewindowForm.get('aggregation.limit').clearValidators();
    } else {
      this.timewindowForm.get('aggregation.limit').setValidators([Validators.min(this.minDatapointsLimit()),
        Validators.max(this.maxDatapointsLimit())]);
    }
    this.timewindowForm.get('aggregation.limit').updateValueAndValidity({emitEvent: false});
  }

  onTimewindowTypeChange() {
    this.timewindowForm.markAsDirty();
    const timewindowFormValue = this.timewindowForm.getRawValue();
    if (this.timewindow.selectedTab === TimewindowType.REALTIME) {
      if (timewindowFormValue.history.historyType !== HistoryWindowType.FIXED) {
        this.timewindowForm.get('realtime').patchValue({
          realtimeType: Object.keys(RealtimeWindowType).includes(HistoryWindowType[timewindowFormValue.history.historyType]) ?
            RealtimeWindowType[HistoryWindowType[timewindowFormValue.history.historyType]] :
            timewindowFormValue.realtime.realtimeType,
          timewindowMs: timewindowFormValue.history.timewindowMs,
          quickInterval: timewindowFormValue.history.quickInterval.startsWith('CURRENT') ?
            timewindowFormValue.history.quickInterval : timewindowFormValue.realtime.quickInterval
        });
        setTimeout(() => this.timewindowForm.get('realtime.interval').patchValue(timewindowFormValue.history.interval));
      }
    } else {
      this.timewindowForm.get('history').patchValue({
        historyType: HistoryWindowType[RealtimeWindowType[timewindowFormValue.realtime.realtimeType]],
        timewindowMs: timewindowFormValue.realtime.timewindowMs,
        quickInterval: timewindowFormValue.realtime.quickInterval
      });
      setTimeout(() => this.timewindowForm.get('history.interval').patchValue(timewindowFormValue.realtime.interval));
    }
    this.timewindowForm.patchValue({
      aggregation: {
        type: timewindowFormValue.aggregation.type,
        limit: timewindowFormValue.aggregation.limit
      },
      timezone: timewindowFormValue.timezone,
      hideInterval: timewindowFormValue.hideInterval,
      hideAggregation: timewindowFormValue.hideAggregation,
      hideAggInterval: timewindowFormValue.hideAggInterval,
      hideTimezone: timewindowFormValue.hideTimezone
    });
  }

  update() {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    this.timewindow = mergeDeep(this.timewindow, timewindowFormValue);
    if (!this.aggregation) {
      delete this.timewindow.aggregation;
    }
    this.dialogRef.close(this.timewindow);
  }

  cancel() {
    this.dialogRef.close();
  }

  minDatapointsLimit() {
    return this.timeService.getMinDatapointsLimit();
  }

  maxDatapointsLimit() {
    return this.timeService.getMaxDatapointsLimit();
  }

  minRealtimeAggInterval() {
    return this.timeService.minIntervalLimit(this.currentRealtimeTimewindow());
  }

  maxRealtimeAggInterval() {
    return this.timeService.maxIntervalLimit(this.currentRealtimeTimewindow());
  }

  currentRealtimeTimewindow(): number {
    const timeWindowFormValue = this.timewindowForm.getRawValue();
    switch (timeWindowFormValue.realtime.realtimeType) {
      case RealtimeWindowType.LAST_INTERVAL:
        return timeWindowFormValue.realtime.timewindowMs;
      case RealtimeWindowType.INTERVAL:
        return quickTimeIntervalPeriod(timeWindowFormValue.realtime.quickInterval);
      default:
        return DAY;
    }
  }

  minHistoryAggInterval() {
    return this.timeService.minIntervalLimit(this.currentHistoryTimewindow());
  }

  maxHistoryAggInterval() {
    return this.timeService.maxIntervalLimit(this.currentHistoryTimewindow());
  }

  currentHistoryTimewindow() {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    if (timewindowFormValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
      return timewindowFormValue.history.timewindowMs;
    } else if (timewindowFormValue.history.historyType === HistoryWindowType.INTERVAL) {
      return quickTimeIntervalPeriod(timewindowFormValue.history.quickInterval);
    } else if (timewindowFormValue.history.fixedTimewindow) {
      return timewindowFormValue.history.fixedTimewindow.endTimeMs -
        timewindowFormValue.history.fixedTimewindow.startTimeMs;
    } else {
      return DAY;
    }
  }

}
