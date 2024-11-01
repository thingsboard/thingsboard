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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import {
  aggregationTranslations,
  AggregationType,
  DAY,
  HistoryWindowType,
  historyWindowTypeTranslations,
  quickTimeIntervalPeriod,
  RealtimeWindowType,
  realtimeWindowTypeTranslations,
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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface TimewindowConfigDialogData {
  quickIntervalOnly: boolean;
  aggregation: boolean;
  timewindow: Timewindow;
}

@Component({
  selector: 'tb-timewindow-config-dialog',
  templateUrl: './timewindow-config-dialog.component.html',
  styleUrls: ['./timewindow-config-dialog.component.scss', './timewindow-form.scss']
})
export class TimewindowConfigDialogComponent extends PageComponent implements OnInit, OnDestroy {

  quickIntervalOnly = false;

  aggregation = false;

  timewindowForm: FormGroup;

  historyTypes = HistoryWindowType;

  realtimeTypes = RealtimeWindowType;

  timewindowTypes = TimewindowType;

  aggregationTypes = AggregationType;

  aggregations = Object.keys(AggregationType);

  aggregationTypesTranslations = aggregationTranslations;

  result: Timewindow;

  timewindowTypeOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant('timewindow.realtime'),
      value: this.timewindowTypes.REALTIME
    },
    {
      name: this.translate.instant('timewindow.history'),
      value: this.timewindowTypes.HISTORY
    }
  ];

  realtimeTimewindowOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant(realtimeWindowTypeTranslations.get(RealtimeWindowType.INTERVAL)),
      value: this.realtimeTypes.INTERVAL
    }
  ];

  historyTimewindowOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant(historyWindowTypeTranslations.get(HistoryWindowType.LAST_INTERVAL)),
      value: this.historyTypes.LAST_INTERVAL
    },
    {
      name: this.translate.instant(historyWindowTypeTranslations.get(HistoryWindowType.FIXED)),
      value: this.historyTypes.FIXED
    },
    {
      name: this.translate.instant(historyWindowTypeTranslations.get(HistoryWindowType.INTERVAL)),
      value: this.historyTypes.INTERVAL
    }
  ];

  realtimeTypeSelectionAvailable: boolean;

  private timewindow: Timewindow;

  private destroy$ = new Subject<void>();

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
        name: this.translate.instant(realtimeWindowTypeTranslations.get(RealtimeWindowType.LAST_INTERVAL)),
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
      selectedTab: [isDefined(this.timewindow.selectedTab) ? this.timewindow.selectedTab : TimewindowType.REALTIME],
      realtime: this.fb.group({
        realtimeType: [ isDefined(realtime?.realtimeType) ? this.timewindow.realtime.realtimeType : RealtimeWindowType.LAST_INTERVAL ],
        timewindowMs: [ isDefined(realtime?.timewindowMs) ? this.timewindow.realtime.timewindowMs : null ],
        interval: [ isDefined(realtime?.interval) ? this.timewindow.realtime.interval : null ],
        quickInterval: [ isDefined(realtime?.quickInterval) ? this.timewindow.realtime.quickInterval : null ],
        disableCustomInterval: [ isDefinedAndNotNull(this.timewindow.realtime?.disableCustomInterval)
          ? this.timewindow.realtime?.disableCustomInterval : false ],
        disableCustomGroupInterval: [ isDefinedAndNotNull(this.timewindow.realtime?.disableCustomGroupInterval)
          ? this.timewindow.realtime?.disableCustomGroupInterval : false ],
        hideInterval: [ isDefinedAndNotNull(this.timewindow.realtime.hideInterval)
          ? this.timewindow.realtime.hideInterval : false ],
        hideLastInterval: [{
          value: isDefinedAndNotNull(this.timewindow.realtime.hideLastInterval)
            ? this.timewindow.realtime.hideLastInterval : false,
          disabled: this.timewindow.realtime.hideInterval
        }],
        hideQuickInterval: [{
          value: isDefinedAndNotNull(this.timewindow.realtime.hideQuickInterval)
            ? this.timewindow.realtime.hideQuickInterval : false,
          disabled: this.timewindow.realtime.hideInterval
        }]
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
        hideInterval: [ isDefinedAndNotNull(this.timewindow.history.hideInterval)
          ? this.timewindow.history.hideInterval : false ],
        hideLastInterval: [{
          value: isDefinedAndNotNull(this.timewindow.history.hideLastInterval)
            ? this.timewindow.history.hideLastInterval : false,
          disabled: this.timewindow.history.hideInterval
        }],
        hideQuickInterval: [{
          value: isDefinedAndNotNull(this.timewindow.history.hideQuickInterval)
            ? this.timewindow.history.hideQuickInterval : false,
          disabled: this.timewindow.history.hideInterval
        }],
        hideFixedInterval: [{
          value: isDefinedAndNotNull(this.timewindow.history.hideFixedInterval)
            ? this.timewindow.history.hideFixedInterval : false,
          disabled: this.timewindow.history.hideInterval
        }]
      }),
      aggregation: this.fb.group({
        type: [ isDefined(aggregation?.type) ? this.timewindow.aggregation.type : null ],
        limit: [ isDefined(aggregation?.limit) ? this.timewindow.aggregation.limit : null ]
      }),
      timezone: [ isDefined(this.timewindow.timezone) ? this.timewindow.timezone : null ],
      hideAggregation: [ isDefinedAndNotNull(this.timewindow.hideAggregation)
                      ? this.timewindow.hideAggregation : false ],
      hideAggInterval: [ isDefinedAndNotNull(this.timewindow.hideAggInterval)
                      ? this.timewindow.hideAggInterval : false ],
      hideTimezone: [ isDefinedAndNotNull(this.timewindow.hideTimezone)
                      ? this.timewindow.hideTimezone : false ]
    });

    this.updateValidators(this.timewindowForm.get('aggregation.type').value);
    this.timewindowForm.get('aggregation.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((aggregationType: AggregationType) => {
      this.updateValidators(aggregationType);
    });
    this.timewindowForm.get('selectedTab').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((selectedTab: TimewindowType) => {
      this.onTimewindowTypeChange(selectedTab);
    });
    this.timewindowForm.get('realtime.hideInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: boolean) => {
      if (value) {
        this.timewindowForm.get('realtime.hideLastInterval').disable({emitEvent: false});
        this.timewindowForm.get('realtime.hideQuickInterval').disable({emitEvent: false});
      } else {
        this.timewindowForm.get('realtime.hideLastInterval').enable({emitEvent: false});
        this.timewindowForm.get('realtime.hideQuickInterval').enable({emitEvent: false});
      }
    });
    this.timewindowForm.get('realtime.hideLastInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideLastInterval: boolean) => {
      if (hideLastInterval && !this.timewindowForm.get('realtime.hideQuickInterval').value) {
        this.timewindowForm.get('realtime.realtimeType').setValue(RealtimeWindowType.INTERVAL);
      }
    });
    this.timewindowForm.get('realtime.hideQuickInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideQuickInterval: boolean) => {
      if (hideQuickInterval && !this.timewindowForm.get('realtime.hideLastInterval').value) {
        this.timewindowForm.get('realtime.realtimeType').setValue(RealtimeWindowType.LAST_INTERVAL);
      }
    });

    this.timewindowForm.get('history.hideInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: boolean) => {
      if (value) {
        this.timewindowForm.get('history.hideLastInterval').disable({emitEvent: false});
        this.timewindowForm.get('history.hideQuickInterval').disable({emitEvent: false});
        this.timewindowForm.get('history.hideFixedInterval').disable({emitEvent: false});
      } else {
        this.timewindowForm.get('history.hideLastInterval').enable({emitEvent: false});
        this.timewindowForm.get('history.hideQuickInterval').enable({emitEvent: false});
        this.timewindowForm.get('history.hideFixedInterval').enable({emitEvent: false});
      }
    });
    this.timewindowForm.get('history.hideLastInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideLastInterval: boolean) => {
      if (hideLastInterval) {
        if (!this.timewindowForm.get('history.hideFixedInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.FIXED);
        } else if (!this.timewindowForm.get('history.hideQuickInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.INTERVAL);
        }
      }
    });
    this.timewindowForm.get('history.hideFixedInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideFixedInterval: boolean) => {
      if (hideFixedInterval) {
        if (!this.timewindowForm.get('history.hideLastInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.LAST_INTERVAL);
        } else if (!this.timewindowForm.get('history.hideQuickInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.INTERVAL);
        }
      }
    });
    this.timewindowForm.get('history.hideQuickInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideQuickInterval: boolean) => {
      if (hideQuickInterval) {
        if (!this.timewindowForm.get('history.hideLastInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.LAST_INTERVAL);
        } else if (!this.timewindowForm.get('history.hideFixedInterval').value) {
          this.timewindowForm.get('history.historyType').setValue(HistoryWindowType.FIXED);
        }
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateValidators(aggType: AggregationType) {
    if (aggType !== AggregationType.NONE) {
      this.timewindowForm.get('aggregation.limit').clearValidators();
    } else {
      this.timewindowForm.get('aggregation.limit').setValidators([Validators.required]);
    }
    this.timewindowForm.get('aggregation.limit').updateValueAndValidity({emitEvent: false});
  }

  private onTimewindowTypeChange(selectedTab: TimewindowType) {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    if (selectedTab === TimewindowType.REALTIME) {
      if (timewindowFormValue.history.historyType !== HistoryWindowType.FIXED
        && !(this.quickIntervalOnly && timewindowFormValue.history.historyType === HistoryWindowType.LAST_INTERVAL)) {

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
