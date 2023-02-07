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

import { Component, Input, OnInit, ViewContainerRef } from '@angular/core';
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
import { isDefined } from '@core/utils';

export interface TimewindowPanelData {
  historyOnly: boolean;
  quickIntervalOnly: boolean;
  timewindow: Timewindow;
  aggregation: boolean;
  timezone: boolean;
  isEdit: boolean;
}

@Component({
  selector: 'tb-timewindow-panel',
  templateUrl: './timewindow-panel.component.html',
  styleUrls: ['./timewindow-panel.component.scss']
})
export class TimewindowPanelComponent extends PageComponent implements OnInit {

  @Input()
  data: any;

  @Input()
  onClose: (result: Timewindow | null) => void;

  historyOnly = false;

  quickIntervalOnly = false;

  aggregation = false;

  timezone = false;

  isEdit = false;

  timewindow: Timewindow;

  timewindowForm: FormGroup;

  historyTypes = HistoryWindowType;

  realtimeTypes = RealtimeWindowType;

  timewindowTypes = TimewindowType;

  aggregationTypes = AggregationType;

  aggregations = Object.keys(AggregationType);

  aggregationTypesTranslations = aggregationTranslations;

  private result: Timewindow;

  constructor(protected store: Store<AppState>,
              public fb: FormBuilder,
              private timeService: TimeService,
              public viewContainerRef: ViewContainerRef) {
    super(store);
  }

  ngOnInit(): void {
    this.historyOnly = this.data.historyOnly;
    this.quickIntervalOnly = this.data.quickIntervalOnly;
    this.timewindow = this.data.timewindow;
    this.aggregation = this.data.aggregation;
    this.timezone = this.data.timezone;
    this.isEdit = this.data.isEdit;

    const hideInterval = this.timewindow.hideInterval || false;
    const hideLastInterval = this.timewindow.hideLastInterval || false;
    const hideQuickInterval = this.timewindow.hideQuickInterval || false;
    const hideAggregation = this.timewindow.hideAggregation || false;
    const hideAggInterval = this.timewindow.hideAggInterval || false;
    const hideTimezone = this.timewindow.hideTimezone || false;

    const realtime = this.timewindow.realtime;
    const history = this.timewindow.history;
    const aggregation = this.timewindow.aggregation;

    this.timewindowForm = this.fb.group({
      realtime: this.fb.group({
        realtimeType: [{
          value: this.defined(realtime, realtime.realtimeType) ? this.timewindow.realtime.realtimeType : RealtimeWindowType.LAST_INTERVAL,
          disabled: hideInterval
        }],
        timewindowMs: [{
          value: this.defined(realtime, realtime.timewindowMs) ? this.timewindow.realtime.timewindowMs : null,
          disabled: hideInterval || hideLastInterval
        }],
        interval: [this.defined(realtime, realtime.interval) ? this.timewindow.realtime.interval : null],
        quickInterval: [{
          value: this.defined(realtime, realtime.quickInterval) ? this.timewindow.realtime.quickInterval : null,
          disabled: hideInterval || hideQuickInterval
        }]
      }),
      history: this.fb.group({
        historyType: [{
          value: this.defined(history, history.historyType) ? this.timewindow.history.historyType : HistoryWindowType.LAST_INTERVAL,
          disabled: hideInterval
        }],
        timewindowMs: [{
          value: this.defined(history, history.timewindowMs) ? this.timewindow.history.timewindowMs : null,
          disabled: hideInterval
        }],
        interval: [ this.defined(history, history.interval) ? this.timewindow.history.interval : null
        ],
        fixedTimewindow: [{
          value: this.defined(history, history.fixedTimewindow) ? this.timewindow.history.fixedTimewindow : null,
          disabled: hideInterval
        }],
        quickInterval: [{
          value: this.defined(history, history.quickInterval) ? this.timewindow.history.quickInterval : null,
          disabled: hideInterval
        }]
      }),
      aggregation: this.fb.group({
        type: [{
          value: this.defined(aggregation, aggregation.type) ? this.timewindow.aggregation.type : null,
          disabled: hideAggregation
        }],
        limit: [{
          value: this.defined(aggregation, aggregation.limit) ? this.checkLimit(this.timewindow.aggregation.limit) : null,
          disabled: hideAggInterval
        }, []]
      }),
      timezone: [{
        value: isDefined(this.timewindow.timezone) ? this.timewindow.timezone : null,
        disabled: hideTimezone
      }]
    });
    this.updateValidators();
    this.timewindowForm.get('aggregation.type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
  }

  private defined(arg1, arg2) {
    return arg1 && isDefined(arg2);
  }

  private checkLimit(limit?: number): number {
    if (!limit || limit < this.minDatapointsLimit()) {
      return this.minDatapointsLimit();
    } else if (limit > this.maxDatapointsLimit()) {
      return this.maxDatapointsLimit();
    }
    return limit;
  }

  private updateValidators() {
    const aggType = this.timewindowForm.get('aggregation.type').value;
    if (aggType !== AggregationType.NONE) {
      this.timewindowForm.get('aggregation.limit').clearValidators();
    } else {
      this.timewindowForm.get('aggregation.limit').setValidators([Validators.min(this.minDatapointsLimit()),
        Validators.max(this.maxDatapointsLimit())]);
    }
    this.timewindowForm.get('aggregation.limit').updateValueAndValidity({emitEvent: false});
  }

  update() {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    this.timewindow.realtime = {
      realtimeType: timewindowFormValue.realtime.realtimeType,
      timewindowMs: timewindowFormValue.realtime.timewindowMs,
      quickInterval: timewindowFormValue.realtime.quickInterval,
      interval: timewindowFormValue.realtime.interval
    };
    this.timewindow.history = {
      historyType: timewindowFormValue.history.historyType,
      timewindowMs: timewindowFormValue.history.timewindowMs,
      interval: timewindowFormValue.history.interval,
      fixedTimewindow: timewindowFormValue.history.fixedTimewindow,
      quickInterval: timewindowFormValue.history.quickInterval,
    };
    if (this.aggregation) {
      this.timewindow.aggregation = {
        type: timewindowFormValue.aggregation.type,
        limit: timewindowFormValue.aggregation.limit
      };
    }
    if (this.timezone) {
      this.timewindow.timezone = timewindowFormValue.timezone;
    }
    this.result = this.timewindow;
    this.cancel(this.result);
  }

  cancel(result: Timewindow | null = null) {
    if (this.onClose) {
      this.onClose(result);
    }
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

  onHideIntervalChanged() {
    if (this.timewindow.hideInterval) {
      this.timewindowForm.get('history.historyType').disable({emitEvent: false});
      this.timewindowForm.get('history.timewindowMs').disable({emitEvent: false});
      this.timewindowForm.get('history.fixedTimewindow').disable({emitEvent: false});
      this.timewindowForm.get('history.quickInterval').disable({emitEvent: false});
      this.timewindowForm.get('realtime.realtimeType').disable({emitEvent: false});
      this.timewindowForm.get('realtime.timewindowMs').disable({emitEvent: false});
      this.timewindowForm.get('realtime.quickInterval').disable({emitEvent: false});
    } else {
      this.timewindowForm.get('history.historyType').enable({emitEvent: false});
      this.timewindowForm.get('history.timewindowMs').enable({emitEvent: false});
      this.timewindowForm.get('history.fixedTimewindow').enable({emitEvent: false});
      this.timewindowForm.get('history.quickInterval').enable({emitEvent: false});
      this.timewindowForm.get('realtime.realtimeType').enable({emitEvent: false});
      if (!this.timewindow.hideLastInterval) {
        this.timewindowForm.get('realtime.timewindowMs').enable({emitEvent: false});
      }
      if (!this.timewindow.hideQuickInterval) {
        this.timewindowForm.get('realtime.quickInterval').enable({emitEvent: false});
      }
    }
    this.timewindowForm.markAsDirty();
  }

  onHideLastIntervalChanged() {
    if (this.timewindow.hideLastInterval) {
      this.timewindowForm.get('realtime.timewindowMs').disable({emitEvent: false});
      if (!this.timewindow.hideQuickInterval) {
        this.timewindowForm.get('realtime.realtimeType').setValue(RealtimeWindowType.INTERVAL);
      }
    } else {
      if (!this.timewindow.hideInterval) {
        this.timewindowForm.get('realtime.timewindowMs').enable({emitEvent: false});
      }
    }
    this.timewindowForm.markAsDirty();
  }

  onHideQuickIntervalChanged() {
    if (this.timewindow.hideQuickInterval) {
      this.timewindowForm.get('realtime.quickInterval').disable({emitEvent: false});
      if (!this.timewindow.hideLastInterval) {
        this.timewindowForm.get('realtime.realtimeType').setValue(RealtimeWindowType.LAST_INTERVAL);
      }
    } else {
      if (!this.timewindow.hideInterval) {
        this.timewindowForm.get('realtime.quickInterval').enable({emitEvent: false});
      }
    }
    this.timewindowForm.markAsDirty();
  }

  onHideAggregationChanged() {
    if (this.timewindow.hideAggregation) {
      this.timewindowForm.get('aggregation.type').disable({emitEvent: false});
    } else {
      this.timewindowForm.get('aggregation.type').enable({emitEvent: false});
    }
    this.timewindowForm.markAsDirty();
  }

  onHideAggIntervalChanged() {
    if (this.timewindow.hideAggInterval) {
      this.timewindowForm.get('aggregation.limit').disable({emitEvent: false});
    } else {
      this.timewindowForm.get('aggregation.limit').enable({emitEvent: false});
    }
    this.timewindowForm.markAsDirty();
  }

  onHideTimezoneChanged() {
    if (this.timewindow.hideTimezone) {
      this.timewindowForm.get('timezone').disable({emitEvent: false});
    } else {
      this.timewindowForm.get('timezone').enable({emitEvent: false});
    }
    this.timewindowForm.markAsDirty();
  }

}
