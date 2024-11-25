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

import { Component, Input, OnInit } from '@angular/core';
import {
  HistoryWindowType,
  QuickTimeInterval, quickTimeIntervalPeriod,
  QuickTimeIntervalTranslationMap,
  RealtimeWindowType,
  TimewindowAllowedAggIntervalOption,
  TimewindowAllowedAggIntervalsConfig,
  TimewindowInterval,
  TimewindowIntervalOption,
  TimewindowType
} from '@shared/models/time/time.models';
import { AbstractControl, FormBuilder, FormGroup, UntypedFormArray } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TimeService } from '@core/services/time.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-interval-options-config-panel',
  templateUrl: './interval-options-config-panel.component.html',
  styleUrls: ['./interval-options-config-panel.component.scss']
})
export class IntervalOptionsConfigPanelComponent implements OnInit {

  @Input()
  @coerceBoolean()
  aggregation = false;

  @Input()
  allowedIntervals: Array<TimewindowInterval>;

  @Input()
  allowedAggIntervals: TimewindowAllowedAggIntervalsConfig;

  @Input()
  intervalType: RealtimeWindowType | HistoryWindowType;

  @Input()
  timewindowType: TimewindowType;

  @Input()
  onClose: (result: Array<any> | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  intervalOptionsConfigForm: FormGroup;

  allIntervals: Array<TimewindowIntervalOption>;
  allIntervalValues: Array<TimewindowInterval>

  private timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  constructor(private fb: FormBuilder,
              private timeService: TimeService,
              private translate: TranslateService) {}

  ngOnInit(): void {
    if (this.intervalType === RealtimeWindowType.LAST_INTERVAL ||
        this.intervalType === HistoryWindowType.LAST_INTERVAL) {
      this.allIntervals = this.timeService.getIntervals(undefined, undefined, false);
      this.allIntervalValues = this.allIntervals.map(interval => interval.value);
    } else {
      this.allIntervalValues = this.getQuickIntervals();
      this.allIntervals = this.allIntervalValues.map(interval => ({
        name: this.timeIntervalTranslationMap.get(interval as QuickTimeInterval),
        value: interval
      }));
    }

    this.intervalOptionsConfigForm = this.fb.group({
      allowedIntervals: [this.allowedIntervals?.length ? this.allowedIntervals : this.allIntervalValues],
      intervals: this.fb.array([])
    });

    const intervalControls: Array<AbstractControl> = [];
    for (const interval of this.allIntervals) {
      const intervalConfig: TimewindowAllowedAggIntervalOption = this.allowedAggIntervals?.hasOwnProperty(interval.value)
        ? this.allIntervalValues[interval.value]
        : null;
      intervalControls.push(this.fb.group({
        name: [this.translate.instant(interval.name, interval.translateParams)],
        value: [interval.value],
        enabled: [this.allowedIntervals?.length ? this.allowedIntervals.includes(interval.value) : true],
        aggIntervals: [intervalConfig ? intervalConfig.aggIntervals : []],
        preferredAggInterval: [intervalConfig ? intervalConfig.preferredAggInterval : null]
      }));
    }
    this.intervalOptionsConfigForm.setControl('intervals', this.fb.array(intervalControls), {emitEvent: false});
  }

  get intervalsFormArray(): UntypedFormArray {
    return this.intervalOptionsConfigForm.get('intervals') as UntypedFormArray;
  }

  minAggInterval(interval: TimewindowInterval) {
    return this.timeService.minIntervalLimit(this.getIntervalMs(interval));
  }

  maxAggInterval(interval: TimewindowInterval) {
    return this.timeService.maxIntervalLimit(this.getIntervalMs(interval));
  }

  private getIntervalMs(interval: TimewindowInterval): number {
    if (this.intervalType === RealtimeWindowType.INTERVAL ||
      this.intervalType === HistoryWindowType.INTERVAL) {
      return quickTimeIntervalPeriod(interval as QuickTimeInterval);
    }
    return interval as number;
  }

  trackByElement(i: number, item: any) {
    return item;
  }

  update() {
    if (this.onClose) {
      const allowedIntervals = this.intervalOptionsConfigForm.get('allowedIntervals').value;
      // if full list selected returns empty for optimization
      this.onClose(allowedIntervals?.length < this.allIntervals.length ? allowedIntervals : []);
    }
  }

  cancel() {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  reset() {
    this.intervalOptionsConfigForm.reset();
    this.intervalOptionsConfigForm.markAsDirty();
  }

  private getQuickIntervals(): Array<QuickTimeInterval> {
    const allQuickIntervals = Object.values(QuickTimeInterval);
    if (this.timewindowType === TimewindowType.REALTIME) {
      return allQuickIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return allQuickIntervals;
  }

}
