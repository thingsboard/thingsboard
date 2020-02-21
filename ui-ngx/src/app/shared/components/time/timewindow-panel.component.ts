///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  Component,
  Inject,
  InjectionToken,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import {
  Aggregation,
  aggregationTranslations,
  AggregationType,
  HistoryWindow,
  HistoryWindowType,
  IntervalWindow,
  Timewindow,
  TimewindowType
} from '@shared/models/time/time.models';
import { DatePipe } from '@angular/common';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TimeService } from '@core/services/time.service';

export const TIMEWINDOW_PANEL_DATA = new InjectionToken<any>('TimewindowPanelData');

export interface TimewindowPanelData {
  historyOnly: boolean;
  timewindow: Timewindow;
  aggregation: boolean;
}

@Component({
  selector: 'tb-timewindow-panel',
  templateUrl: './timewindow-panel.component.html',
  styleUrls: ['./timewindow-panel.component.scss']
})
export class TimewindowPanelComponent extends PageComponent implements OnInit {

  historyOnly = false;

  aggregation = false;

  timewindow: Timewindow;

  result: Timewindow;

  timewindowForm: FormGroup;

  historyTypes = HistoryWindowType;

  timewindowTypes = TimewindowType;

  aggregationTypes = AggregationType;

  aggregations = Object.keys(AggregationType);

  aggregationTypesTranslations = aggregationTranslations;

  constructor(@Inject(TIMEWINDOW_PANEL_DATA) public data: TimewindowPanelData,
              public overlayRef: OverlayRef,
              protected store: Store<AppState>,
              public fb: FormBuilder,
              private timeService: TimeService,
              private translate: TranslateService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private overlay: Overlay,
              public viewContainerRef: ViewContainerRef) {
    super(store);
    this.historyOnly = data.historyOnly;
    this.timewindow = data.timewindow;
    this.aggregation = data.aggregation;
  }

  ngOnInit(): void {
    this.timewindowForm = this.fb.group({
        realtime: this.fb.group(
          {
            timewindowMs: [
              this.timewindow.realtime && typeof this.timewindow.realtime.timewindowMs !== 'undefined'
                ? this.timewindow.realtime.timewindowMs : null
            ],
            interval: [
              this.timewindow.realtime && typeof this.timewindow.realtime.interval !== 'undefined'
                ? this.timewindow.realtime.interval : null
            ]
          }
        ),
        history: this.fb.group(
          {
            historyType: [
              this.timewindow.history && typeof this.timewindow.history.historyType !== 'undefined'
                ? this.timewindow.history.historyType : HistoryWindowType.LAST_INTERVAL
            ],
            timewindowMs: [
              this.timewindow.history && typeof this.timewindow.history.timewindowMs !== 'undefined'
                ? this.timewindow.history.timewindowMs : null
            ],
            interval: [
              this.timewindow.history && typeof this.timewindow.history.interval !== 'undefined'
                ? this.timewindow.history.interval : null
            ],
            fixedTimewindow: [
              this.timewindow.history && typeof this.timewindow.history.fixedTimewindow !== 'undefined'
                ? this.timewindow.history.fixedTimewindow : null
            ]
          }
        ),
        aggregation: this.fb.group(
          {
            type: [
              this.timewindow.aggregation && typeof this.timewindow.aggregation.type !== 'undefined'
                ? this.timewindow.aggregation.type : null
            ],
            limit: [
              this.timewindow.aggregation && typeof this.timewindow.aggregation.limit !== 'undefined'
                ? this.timewindow.aggregation.limit : null,
              [Validators.min(this.minDatapointsLimit()), Validators.max(this.maxDatapointsLimit())]
            ]
          }
        )
    });
  }

  update() {
    const timewindowFormValue = this.timewindowForm.value;
    this.timewindow.realtime = {
      timewindowMs: timewindowFormValue.realtime.timewindowMs,
      interval: timewindowFormValue.realtime.interval
    };
    this.timewindow.history = {
      historyType: timewindowFormValue.history.historyType,
      timewindowMs: timewindowFormValue.history.timewindowMs,
      interval: timewindowFormValue.history.interval,
      fixedTimewindow: timewindowFormValue.history.fixedTimewindow
    };
    if (this.aggregation) {
      this.timewindow.aggregation = {
        type: timewindowFormValue.aggregation.type,
        limit: timewindowFormValue.aggregation.limit
      };
    }
    this.result = this.timewindow;
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }

  minDatapointsLimit() {
    return this.timeService.getMinDatapointsLimit();
  }

  maxDatapointsLimit() {
    return this.timeService.getMaxDatapointsLimit();
  }

  minRealtimeAggInterval() {
    return this.timeService.minIntervalLimit(this.timewindowForm.get('realtime').get('timewindowMs').value);
  }

  maxRealtimeAggInterval() {
    return this.timeService.maxIntervalLimit(this.timewindowForm.get('realtime').get('timewindowMs').value);
  }

  minHistoryAggInterval() {
    return this.timeService.minIntervalLimit(this.currentHistoryTimewindow());
  }

  maxHistoryAggInterval() {
    return this.timeService.maxIntervalLimit(this.currentHistoryTimewindow());
  }

  currentHistoryTimewindow() {
    const timewindowFormValue = this.timewindowForm.value;
    if (timewindowFormValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
      return timewindowFormValue.history.timewindowMs;
    } else {
      return timewindowFormValue.history.fixedTimewindow.endTimeMs -
        timewindowFormValue.history.fixedTimewindow.startTimeMs;
    }
  }

}
