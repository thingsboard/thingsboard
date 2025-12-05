///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import {
  AggregationType,
  currentHistoryTimewindow,
  currentRealtimeTimewindow,
  historyAllowedAggIntervals,
  HistoryWindowType,
  historyWindowTypeTranslations,
  Interval,
  realtimeAllowedAggIntervals,
  RealtimeWindowType,
  realtimeWindowTypeTranslations,
  Timewindow,
  TimewindowAggIntervalsConfig,
  TimewindowType,
  updateFormValuesOnTimewindowTypeChange
} from '@shared/models/time/time.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import {
  deepClean,
  deepClone,
  deleteFalseProperties,
  isDefined,
  isDefinedAndNotNull,
  isEmpty,
  mergeDeepIgnoreArray
} from '@core/utils';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { TranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  AggregationOptionsConfigPanelComponent
} from '@shared/components/time/aggregation/aggregation-options-config-panel.component';
import {
  IntervalOptionsConfigPanelComponent,
  IntervalOptionsConfigPanelData
} from '@shared/components/time/interval-options-config-panel.component';

export interface TimewindowConfigDialogData {
  quickIntervalOnly: boolean;
  aggregation: boolean;
  showSaveAsDefault: boolean;
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

  showSaveAsDefault = false;

  timewindowForm: FormGroup;

  historyTypes = HistoryWindowType;

  realtimeTypes = RealtimeWindowType;

  timewindowTypes = TimewindowType;

  aggregationTypes = AggregationType;

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
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              public viewContainerRef: ViewContainerRef) {
    super(store);
    this.quickIntervalOnly = data.quickIntervalOnly;
    this.aggregation = data.aggregation;
    this.showSaveAsDefault = data.showSaveAsDefault;
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
        realtimeType: [ this.quickIntervalOnly
          ? RealtimeWindowType.INTERVAL
          : (isDefined(realtime?.realtimeType) ? realtime.realtimeType : RealtimeWindowType.LAST_INTERVAL) ],
        timewindowMs: [ isDefined(realtime?.timewindowMs) ? realtime.timewindowMs : null ],
        interval: [ isDefined(realtime?.interval) ? realtime.interval : null ],
        quickInterval: [ isDefined(realtime?.quickInterval) ? realtime.quickInterval : null ],
        disableCustomInterval: [ isDefinedAndNotNull(realtime?.disableCustomInterval)
          ? realtime.disableCustomInterval : false ],
        disableCustomGroupInterval: [ isDefinedAndNotNull(realtime?.disableCustomGroupInterval)
          ? realtime.disableCustomGroupInterval : false ],
        hideInterval: [ isDefinedAndNotNull(realtime?.hideInterval)
          ? realtime.hideInterval : false ],
        hideLastInterval: [{
          value: isDefinedAndNotNull(realtime?.hideLastInterval)
            ? realtime.hideLastInterval : false,
          disabled: realtime?.hideInterval
        }],
        hideQuickInterval: [{
          value: isDefinedAndNotNull(realtime?.hideQuickInterval)
            ? realtime.hideQuickInterval : false,
          disabled: realtime?.hideInterval
        }],
        advancedParams: this.fb.group({
          allowedLastIntervals: [ isDefinedAndNotNull(realtime?.advancedParams?.allowedLastIntervals)
            ? realtime.advancedParams.allowedLastIntervals : null ],
          allowedQuickIntervals: [ isDefinedAndNotNull(realtime?.advancedParams?.allowedQuickIntervals)
            ? realtime.advancedParams.allowedQuickIntervals : null ],
          lastAggIntervalsConfig: [ isDefinedAndNotNull(realtime?.advancedParams?.lastAggIntervalsConfig)
            ? realtime.advancedParams.lastAggIntervalsConfig : null ],
          quickAggIntervalsConfig: [ isDefinedAndNotNull(realtime?.advancedParams?.quickAggIntervalsConfig)
            ? realtime.advancedParams.quickAggIntervalsConfig : null ]
        })
      }),
      history: this.fb.group({
        historyType: [ isDefined(history?.historyType) ? history.historyType : HistoryWindowType.LAST_INTERVAL ],
        timewindowMs: [ isDefined(history?.timewindowMs) ? history.timewindowMs : null ],
        interval: [ isDefined(history?.interval) ? history.interval : null ],
        fixedTimewindow: [ isDefined(history?.fixedTimewindow) ? history.fixedTimewindow : null ],
        quickInterval: [ isDefined(history?.quickInterval) ? history.quickInterval : null ],
        disableCustomInterval: [ isDefinedAndNotNull(history?.disableCustomInterval)
          ? history.disableCustomInterval : false ],
        disableCustomGroupInterval: [ isDefinedAndNotNull(history?.disableCustomGroupInterval)
          ? history.disableCustomGroupInterval : false ],
        hideInterval: [ isDefinedAndNotNull(history?.hideInterval)
          ? history.hideInterval : false ],
        hideLastInterval: [{
          value: isDefinedAndNotNull(history?.hideLastInterval)
            ? history.hideLastInterval : false,
          disabled: history?.hideInterval
        }],
        hideQuickInterval: [{
          value: isDefinedAndNotNull(history?.hideQuickInterval)
            ? history.hideQuickInterval : false,
          disabled: history?.hideInterval
        }],
        hideFixedInterval: [{
          value: isDefinedAndNotNull(history?.hideFixedInterval)
            ? history.hideFixedInterval : false,
          disabled: history?.hideInterval
        }],
        advancedParams: this.fb.group({
          allowedLastIntervals: [ isDefinedAndNotNull(history?.advancedParams?.allowedLastIntervals)
            ? history.advancedParams.allowedLastIntervals : null ],
          allowedQuickIntervals: [ isDefinedAndNotNull(history?.advancedParams?.allowedQuickIntervals)
            ? history.advancedParams.allowedQuickIntervals : null ],
          lastAggIntervalsConfig: [ isDefinedAndNotNull(history?.advancedParams?.lastAggIntervalsConfig)
            ? history.advancedParams.lastAggIntervalsConfig : null ],
          quickAggIntervalsConfig: [ isDefinedAndNotNull(history?.advancedParams?.quickAggIntervalsConfig)
            ? history.advancedParams.quickAggIntervalsConfig : null ]
        })
      }),
      aggregation: this.fb.group({
        type: [ isDefined(aggregation?.type) ? this.timewindow.aggregation.type : null ],
        limit: [ isDefined(aggregation?.limit) ? this.timewindow.aggregation.limit : null ]
      }),
      timezone: [ isDefined(this.timewindow.timezone) ? this.timewindow.timezone : null ],
      allowedAggTypes: [ isDefinedAndNotNull(this.timewindow.allowedAggTypes)
                      ? this.timewindow.allowedAggTypes : null ],
      hideAggregation: [ isDefinedAndNotNull(this.timewindow.hideAggregation)
                      ? this.timewindow.hideAggregation : false ],
      hideAggInterval: [ isDefinedAndNotNull(this.timewindow.hideAggInterval)
                      ? this.timewindow.hideAggInterval : false ],
      hideTimezone: [ isDefinedAndNotNull(this.timewindow.hideTimezone)
                      ? this.timewindow.hideTimezone : false ],
      hideSaveAsDefault: [ isDefinedAndNotNull(this.timewindow.hideSaveAsDefault)
        ? this.timewindow.hideSaveAsDefault : false ],
    });
    this.updateValidators(this.timewindowForm.get('aggregation.type').value);

    if (this.aggregation) {
      this.timewindowForm.get('realtime.timewindowMs').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((timewindowMs: number) => {
        const lastAggIntervalsConfig: TimewindowAggIntervalsConfig =
          this.timewindowForm.get('realtime.advancedParams.lastAggIntervalsConfig').value;
        if (lastAggIntervalsConfig?.hasOwnProperty(timewindowMs) &&
          lastAggIntervalsConfig[timewindowMs].defaultAggInterval) {
          setTimeout(() => this.timewindowForm.get('realtime.interval').patchValue(
            lastAggIntervalsConfig[timewindowMs].defaultAggInterval, {emitEvent: false}
          ));
        }
      });
      this.timewindowForm.get('realtime.quickInterval').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((quickInterval: number) => {
        const quickAggIntervalsConfig: TimewindowAggIntervalsConfig =
          this.timewindowForm.get('realtime.advancedParams.quickAggIntervalsConfig').value;
        if (quickAggIntervalsConfig?.hasOwnProperty(quickInterval) &&
          quickAggIntervalsConfig[quickInterval].defaultAggInterval) {
          setTimeout(() => this.timewindowForm.get('realtime.interval').patchValue(
            quickAggIntervalsConfig[quickInterval].defaultAggInterval, {emitEvent: false}
          ));
        }
      });
      this.timewindowForm.get('history.timewindowMs').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((timewindowMs: number) => {
        const lastAggIntervalsConfig: TimewindowAggIntervalsConfig =
          this.timewindowForm.get('history.advancedParams.lastAggIntervalsConfig').value;
        if (lastAggIntervalsConfig?.hasOwnProperty(timewindowMs) &&
          lastAggIntervalsConfig[timewindowMs].defaultAggInterval) {
          setTimeout(() => this.timewindowForm.get('history.interval').patchValue(
            lastAggIntervalsConfig[timewindowMs].defaultAggInterval, {emitEvent: false}
          ));
        }
      });
      this.timewindowForm.get('history.quickInterval').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((quickInterval: number) => {
        const quickAggIntervalsConfig: TimewindowAggIntervalsConfig =
          this.timewindowForm.get('history.advancedParams.quickAggIntervalsConfig').value;
        if (quickAggIntervalsConfig?.hasOwnProperty(quickInterval) &&
          quickAggIntervalsConfig[quickInterval].defaultAggInterval) {
          setTimeout(() => this.timewindowForm.get('history.interval').patchValue(
            quickAggIntervalsConfig[quickInterval].defaultAggInterval, {emitEvent: false}
          ));
        }
      });

      this.timewindowForm.get('aggregation.type').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((aggregationType: AggregationType) => {
        this.updateValidators(aggregationType);
      });
    }

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
      this.updateDisableAdvancedOptionState('realtime.disableCustomInterval', value);
    });
    this.timewindowForm.get('realtime.hideLastInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hideLastInterval: boolean) => {
      if (hideLastInterval && !this.timewindowForm.get('realtime.hideQuickInterval').value) {
        this.timewindowForm.get('realtime.realtimeType').setValue(RealtimeWindowType.INTERVAL);
      }
      this.updateDisableAdvancedOptionState('realtime.disableCustomInterval', hideLastInterval);
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
      this.updateDisableAdvancedOptionState('history.disableCustomInterval', value);
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
      this.updateDisableAdvancedOptionState('history.disableCustomInterval', hideLastInterval);
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

    this.timewindowForm.get('hideAggInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: boolean) => {
      this.updateDisableAdvancedOptionState('realtime.disableCustomGroupInterval', value);
      this.updateDisableAdvancedOptionState('history.disableCustomGroupInterval', value);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateDisableAdvancedOptionState(controlName: string, intervalHidden: boolean) {
    if (intervalHidden) {
      this.timewindowForm.get(controlName).disable({emitEvent: false});
      this.timewindowForm.get(controlName).patchValue(false, {emitEvent: false});
    } else {
      this.timewindowForm.get(controlName).enable({emitEvent: false});
    }
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
    const realtimeDisableCustomInterval = timewindowFormValue.realtime.disableCustomInterval;
    const historyDisableCustomInterval = timewindowFormValue.history.disableCustomInterval;
    updateFormValuesOnTimewindowTypeChange(selectedTab, this.timewindowForm,
      realtimeDisableCustomInterval, historyDisableCustomInterval,
      timewindowFormValue.realtime.advancedParams, timewindowFormValue.history.advancedParams,
      this.realtimeTimewindowOptions, this.historyTimewindowOptions);
  }

  update() {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    this.timewindow = mergeDeepIgnoreArray(this.timewindow, timewindowFormValue);

    const realtimeConfigurableLastIntervalsAvailable = !(timewindowFormValue.hideAggInterval &&
      (timewindowFormValue.realtime.hideInterval || timewindowFormValue.realtime.hideLastInterval));
    const realtimeConfigurableQuickIntervalsAvailable = !(timewindowFormValue.hideAggInterval &&
      (timewindowFormValue.realtime.hideInterval || timewindowFormValue.realtime.hideQuickInterval));
    const historyConfigurableLastIntervalsAvailable = !(timewindowFormValue.hideAggInterval &&
      (timewindowFormValue.history.hideInterval || timewindowFormValue.history.hideLastInterval));
    const historyConfigurableQuickIntervalsAvailable = !(timewindowFormValue.hideAggInterval &&
      (timewindowFormValue.history.hideInterval || timewindowFormValue.history.hideQuickInterval));

    if (!realtimeConfigurableLastIntervalsAvailable) {
      delete this.timewindow.realtime.advancedParams.allowedLastIntervals;
    }
    if (!realtimeConfigurableQuickIntervalsAvailable) {
      delete this.timewindow.realtime.advancedParams.allowedQuickIntervals;
    }
    if (realtimeConfigurableLastIntervalsAvailable && !isEmpty(timewindowFormValue.realtime.advancedParams.lastAggIntervalsConfig)) {
      this.timewindow.realtime.advancedParams.lastAggIntervalsConfig = timewindowFormValue.realtime.advancedParams.lastAggIntervalsConfig;
    } else {
      delete this.timewindow.realtime.advancedParams.lastAggIntervalsConfig;
    }
    if (realtimeConfigurableQuickIntervalsAvailable && !isEmpty(timewindowFormValue.realtime.advancedParams.quickAggIntervalsConfig)) {
      this.timewindow.realtime.advancedParams.quickAggIntervalsConfig = timewindowFormValue.realtime.advancedParams.quickAggIntervalsConfig;
    } else {
      delete this.timewindow.realtime.advancedParams.quickAggIntervalsConfig;
    }

    if (!historyConfigurableLastIntervalsAvailable) {
      delete this.timewindow.history.advancedParams.allowedLastIntervals;
    }
    if (!historyConfigurableQuickIntervalsAvailable) {
      delete this.timewindow.history.advancedParams.allowedQuickIntervals;
    }
    if (historyConfigurableLastIntervalsAvailable && !isEmpty(timewindowFormValue.history.advancedParams.lastAggIntervalsConfig)) {
      this.timewindow.history.advancedParams.lastAggIntervalsConfig = timewindowFormValue.history.advancedParams.lastAggIntervalsConfig;
    } else {
      delete this.timewindow.history.advancedParams.lastAggIntervalsConfig;
    }
    if (historyConfigurableQuickIntervalsAvailable && !isEmpty(timewindowFormValue.history.advancedParams.quickAggIntervalsConfig)) {
      this.timewindow.history.advancedParams.quickAggIntervalsConfig = timewindowFormValue.history.advancedParams.quickAggIntervalsConfig;
    } else {
      delete this.timewindow.history.advancedParams.quickAggIntervalsConfig;
    }

    if (timewindowFormValue.hideAggregation) {
      delete this.timewindow.allowedAggTypes;
    }

    if (!this.aggregation) {
      delete this.timewindow.aggregation;
    }

    deleteFalseProperties(this.timewindow);
    this.dialogRef.close(deepClean(this.timewindow));
  }

  cancel() {
    this.dialogRef.close();
  }

  get minRealtimeAggInterval() {
    return this.timeService.minIntervalLimit(this.currentRealtimeTimewindow());
  }

  get maxRealtimeAggInterval() {
    return this.timeService.maxIntervalLimit(this.currentRealtimeTimewindow());
  }

  private currentRealtimeTimewindow(): number {
    return currentRealtimeTimewindow(this.timewindowForm.getRawValue());
  }

  get minHistoryAggInterval() {
    return this.timeService.minIntervalLimit(this.currentHistoryTimewindow());
  }

  get maxHistoryAggInterval() {
    return this.timeService.maxIntervalLimit(this.currentHistoryTimewindow());
  }

  private currentHistoryTimewindow(): number {
    return currentHistoryTimewindow(this.timewindowForm.getRawValue());
  }

  get realtimeAllowedAggIntervals(): Array<Interval> {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    return realtimeAllowedAggIntervals(timewindowFormValue, timewindowFormValue.realtime.advancedParams);
  }

  get historyAllowedAggIntervals(): Array<Interval> {
    const timewindowFormValue = this.timewindowForm.getRawValue();
    return historyAllowedAggIntervals(timewindowFormValue, timewindowFormValue.history.advancedParams);
  }

  openAggregationOptionsConfig($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = ($event.target || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const aggregationConfigPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: AggregationOptionsConfigPanelComponent,
        preferredPlacement: ['left', 'leftTop', 'leftBottom'],
        context: {
          allowedAggregationTypes: deepClone(this.timewindowForm.get('allowedAggTypes').value),
          onClose: (result: Array<AggregationType> | null) => {
            aggregationConfigPopover.hide();
            if (result) {
              this.timewindowForm.get('allowedAggTypes').patchValue(result);
              this.timewindowForm.markAsDirty();
            }
          }
        },
        overlayStyle: {maxHeight: '500px', height: '100%'},
        popoverContentStyle: {padding: 0},
        isModal: true
      });
      aggregationConfigPopover.tbComponentRef.instance.popoverComponent = aggregationConfigPopover;
    }
    this.cd.detectChanges();
  }

  configureRealtimeLastIntervalOptions($event: Event) {
    this.openIntervalOptionsConfig($event,
      'realtime.advancedParams.allowedLastIntervals', 'realtime.advancedParams.lastAggIntervalsConfig',
      RealtimeWindowType.LAST_INTERVAL);
  }

  configureRealtimeQuickIntervalOptions($event: Event) {
    this.openIntervalOptionsConfig($event,
      'realtime.advancedParams.allowedQuickIntervals', 'realtime.advancedParams.quickAggIntervalsConfig',
      RealtimeWindowType.INTERVAL, TimewindowType.REALTIME);
  }

  configureHistoryLastIntervalOptions($event: Event) {
    this.openIntervalOptionsConfig($event,
      'history.advancedParams.allowedLastIntervals', 'history.advancedParams.lastAggIntervalsConfig',
      HistoryWindowType.LAST_INTERVAL);
  }

  configureHistoryQuickIntervalOptions($event: Event) {
    this.openIntervalOptionsConfig($event,
      'history.advancedParams.allowedQuickIntervals', 'history.advancedParams.quickAggIntervalsConfig',
      HistoryWindowType.INTERVAL, TimewindowType.HISTORY);
  }

  private openIntervalOptionsConfig($event: Event,
                                    allowedIntervalsControlName: string,
                                    aggIntervalsConfigControlName: string,
                                    intervalType: RealtimeWindowType | HistoryWindowType, timewindowType?: TimewindowType) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = ($event.target || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const intervalsConfigPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: IntervalOptionsConfigPanelComponent,
        preferredPlacement: ['left', 'leftTop', 'leftBottom'],
        context: {
          aggregation: this.aggregation,
          allowedIntervals: deepClone(this.timewindowForm.get(allowedIntervalsControlName).value),
          aggIntervalsConfig: deepClone(this.timewindowForm.get(aggIntervalsConfigControlName).value),
          intervalType: intervalType,
          timewindowType: timewindowType,
          onClose: (result: IntervalOptionsConfigPanelData | null) => {
            intervalsConfigPopover.hide();
            if (result) {
              this.timewindowForm.get(allowedIntervalsControlName).patchValue(result.allowedIntervals);
              this.timewindowForm.get(aggIntervalsConfigControlName).patchValue(result.aggIntervalsConfig);
              this.timewindowForm.markAsDirty();
            }
          }
        },
        overlayStyle: {maxHeight: '500px', height: '100%'},
        popoverContentStyle: {padding: 0},
        isModal: true
      });
      intervalsConfigPopover.tbComponentRef.instance.popoverComponent = intervalsConfigPopover;
    }
    this.cd.detectChanges();
  }
}
