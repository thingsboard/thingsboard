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

import { Component, DestroyRef, inject, Input, OnInit } from '@angular/core';
import {
  HistoryWindowType,
  QuickTimeInterval, quickTimeIntervalPeriod,
  QuickTimeIntervalTranslationMap,
  RealtimeWindowType,
  TimewindowAggIntervalOptions,
  TimewindowAggIntervalsConfig,
  TimewindowInterval,
  TimewindowIntervalOption,
  TimewindowType
} from '@shared/models/time/time.models';
import { AbstractControl, FormBuilder, FormGroup, UntypedFormArray } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TimeService } from '@core/services/time.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface IntervalOptionsConfigPanelData {
  allowedIntervals: Array<TimewindowInterval>;
  aggIntervalsConfig: TimewindowAggIntervalsConfig
}

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
  aggIntervalsConfig: TimewindowAggIntervalsConfig;

  @Input()
  intervalType: RealtimeWindowType | HistoryWindowType;

  @Input()
  timewindowType: TimewindowType;

  @Input()
  onClose: (result: IntervalOptionsConfigPanelData | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  intervalOptionsConfigForm: FormGroup;

  allIntervals: Array<TimewindowIntervalOption>;
  allIntervalValues: Array<TimewindowInterval>

  private timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  private destroyRef = inject(DestroyRef);

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
      intervals: this.fb.array([])
    });

    const intervalControls: Array<AbstractControl> = [];
    for (const interval of this.allIntervals) {
      const intervalConfig: TimewindowAggIntervalOptions = this.aggIntervalsConfig?.hasOwnProperty(interval.value)
        ? this.aggIntervalsConfig[interval.value]
        : null;
      const intervalEnabled = this.allowedIntervals?.length ? this.allowedIntervals.includes(interval.value) : true;
      const intervalControl = this.fb.group({
        name: [this.translate.instant(interval.name, interval.translateParams)],
        value: [interval.value],
        enabled: [intervalEnabled],
        aggIntervalsConfig: [{value: {
            aggIntervals: intervalConfig?.aggIntervals ? intervalConfig.aggIntervals : [],
            defaultAggInterval: intervalConfig?.defaultAggInterval ? intervalConfig.defaultAggInterval : null
          }, disabled: !(intervalEnabled && this.aggregation)}]
      });
      if (this.aggregation) {
        intervalControl.get('enabled').valueChanges.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe((intervalEnabled) => {
          if (intervalEnabled) {
            intervalControl.get('aggIntervalsConfig').enable({emitEvent: false});
          } else {
            intervalControl.get('aggIntervalsConfig').disable({emitEvent: false});
          }
        });
      }
      intervalControls.push(intervalControl);
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
      const allowedIntervals: Array<TimewindowInterval> = [];
      const aggIntervalsConfig: TimewindowAggIntervalsConfig = {};
      const intervalOptionsConfig = this.intervalOptionsConfigForm.get('intervals').value;
      for (const interval of intervalOptionsConfig) {
        if (interval.enabled) {
          allowedIntervals.push(interval.value);
          if (this.aggregation && (interval.aggIntervalsConfig.aggIntervals.length || interval.aggIntervalsConfig.defaultAggInterval)) {
            const intervalParams: TimewindowAggIntervalOptions = {};
            if (interval.aggIntervalsConfig.aggIntervals.length) {
              intervalParams.aggIntervals = interval.aggIntervalsConfig.aggIntervals;
            }
            if (interval.aggIntervalsConfig.defaultAggInterval) {
              intervalParams.defaultAggInterval = interval.aggIntervalsConfig.defaultAggInterval;
            }
            aggIntervalsConfig[interval.value] = intervalParams;
          }
        }
      }
      this.onClose({
        // if full list selected returns empty for optimization
        allowedIntervals: allowedIntervals?.length < this.allIntervals.length ? allowedIntervals : [],
        aggIntervalsConfig
      });
    }
  }

  cancel() {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  reset() {
    const intervalControls = this.intervalsFormArray.controls;
    for (const interval of intervalControls) {
      interval.patchValue({
        enabled: true,
        aggIntervalsConfig: {
          aggIntervals: [],
          defaultAggInterval: null
        }
      });
    }
    this.intervalOptionsConfigForm.markAsDirty();
  }

  private getQuickIntervals(): Array<QuickTimeInterval> {
    const allQuickIntervals = Object.values(QuickTimeInterval);
    if (this.timewindowType === TimewindowType.REALTIME) {
      return allQuickIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return allQuickIntervals;
  }

  getIndeterminate(): boolean {
    const enabledIntervals = this.intervalsFormArray.value.filter(interval => interval.enabled);
    return enabledIntervals.length !== 0 && enabledIntervals.length !== this.allIntervals.length;
  }

  enableDisableIntervals(allEnabled: boolean) {
    const intervalControls = this.intervalsFormArray.controls;
    for (const interval of intervalControls) {
      interval.patchValue({
        enabled: allEnabled
      });
    }
    this.intervalOptionsConfigForm.markAsDirty();
  }

  getChecked(): boolean {
    const intervals = this.intervalsFormArray.value;
    return intervals.every(interval => interval.enabled);
  }

}
