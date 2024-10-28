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
  QuickTimeInterval,
  QuickTimeIntervalTranslationMap,
  RealtimeWindowType,
  TimewindowIntervalOption,
  TimewindowType
} from '@shared/models/time/time.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TimeService } from '@core/services/time.service';
import { coerceBoolean } from '@shared/decorators/coercion';

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
  allowedIntervals: Array<any>;

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

  private timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  constructor(private fb: FormBuilder,
              private timeService: TimeService) {}

  ngOnInit(): void {
    if (this.intervalType === RealtimeWindowType.LAST_INTERVAL ||
        this.intervalType === HistoryWindowType.LAST_INTERVAL) {
      this.allIntervals = this.timeService.getIntervals(undefined, undefined, false);
    } else {
      const quickIntervals = this.getQuickIntervals();
      this.allIntervals = quickIntervals.map(interval => ({
        name: this.timeIntervalTranslationMap.get(interval),
        value: interval
      }));
    }

    this.intervalOptionsConfigForm = this.fb.group({
      allowedIntervals: [this.allowedIntervals]
    });
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

  private getQuickIntervals() {
    const allQuickIntervals = Object.values(QuickTimeInterval);
    if (this.timewindowType === TimewindowType.REALTIME) {
      return allQuickIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return allQuickIntervals;
  }

}
