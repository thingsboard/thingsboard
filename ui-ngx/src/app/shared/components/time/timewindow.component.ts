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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  StaticProvider,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import {
  cloneSelectedTimewindow,
  getTimezoneInfo,
  HistoryWindowType,
  initModelFromDefaultTimewindow,
  QuickTimeIntervalTranslationMap,
  RealtimeWindowType,
  Timewindow,
  TimewindowType
} from '@shared/models/time/time.models';
import { DatePipe } from '@angular/common';
import { TIMEWINDOW_PANEL_DATA, TimewindowPanelComponent } from '@shared/components/time/timewindow-panel.component';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';

// @dynamic
@Component({
  selector: 'tb-timewindow',
  templateUrl: './timewindow.component.html',
  styleUrls: ['./timewindow.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimewindowComponent),
      multi: true
    }
  ]
})
export class TimewindowComponent implements OnInit, OnDestroy, ControlValueAccessor {

  historyOnlyValue = false;

  @Input()
  set historyOnly(val) {
    const newHistoryOnlyValue = coerceBooleanProperty(val);
    if (this.historyOnlyValue !== newHistoryOnlyValue) {
      this.historyOnlyValue = newHistoryOnlyValue;
      if (this.onHistoryOnlyChanged()) {
        this.notifyChanged();
      }
    }
  }

  get historyOnly() {
    return this.historyOnlyValue;
  }

  alwaysDisplayTypePrefixValue = false;

  @Input()
  set alwaysDisplayTypePrefix(val) {
    this.alwaysDisplayTypePrefixValue = coerceBooleanProperty(val);
  }

  get alwaysDisplayTypePrefix() {
    return this.alwaysDisplayTypePrefixValue;
  }

  quickIntervalOnlyValue = false;

  @Input()
  set quickIntervalOnly(val) {
    this.quickIntervalOnlyValue = coerceBooleanProperty(val);
  }

  get quickIntervalOnly() {
    return this.quickIntervalOnlyValue;
  }

  aggregationValue = false;

  @Input()
  set aggregation(val) {
    this.aggregationValue = coerceBooleanProperty(val);
  }

  get aggregation() {
    return this.aggregationValue;
  }

  timezoneValue = false;

  @Input()
  set timezone(val) {
    this.timezoneValue = coerceBooleanProperty(val);
  }

  get timezone() {
    return this.timezoneValue;
  }

  isToolbarValue = false;

  @Input()
  set isToolbar(val) {
    this.isToolbarValue = coerceBooleanProperty(val);
  }

  get isToolbar() {
    return this.isToolbarValue;
  }

  asButtonValue = false;

  @Input()
  set asButton(val) {
    this.asButtonValue = coerceBooleanProperty(val);
  }

  get asButton() {
    return this.asButtonValue;
  }

  isEditValue = false;

  @Input()
  set isEdit(val) {
    this.isEditValue = coerceBooleanProperty(val);
    this.timewindowDisabled = this.isTimewindowDisabled();
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  direction: 'left' | 'right' = 'left';

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  innerValue: Timewindow;

  timewindowDisabled: boolean;

  private propagateChange = (_: any) => {};

  constructor(private overlay: Overlay,
              private translate: TranslateService,
              private timeService: TimeService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private cd: ChangeDetectorRef,
              private nativeElement: ElementRef,
              public viewContainerRef: ViewContainerRef,
              public breakpointObserver: BreakpointObserver) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  toggleTimewindow($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-timewindow-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content'
    });
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(this.nativeElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: TIMEWINDOW_PANEL_DATA,
        useValue: {
          timewindow: deepClone(this.innerValue),
          historyOnly: this.historyOnly,
          quickIntervalOnly: this.quickIntervalOnly,
          aggregation: this.aggregation,
          timezone: this.timezone,
          isEdit: this.isEdit
        }
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(TimewindowPanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.timewindowDisabled = this.isTimewindowDisabled();
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
    this.cd.detectChanges();
  }

  private onHistoryOnlyChanged(): boolean {
    if (this.historyOnlyValue && this.innerValue) {
      if (this.innerValue.selectedTab !== TimewindowType.HISTORY) {
        this.innerValue.selectedTab = TimewindowType.HISTORY;
        this.updateDisplayValue();
        return true;
      }
    }
    return false;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.timewindowDisabled = this.isTimewindowDisabled();
  }

  writeValue(obj: Timewindow): void {
    this.innerValue = initModelFromDefaultTimewindow(obj, this.quickIntervalOnly, this.timeService);
    this.timewindowDisabled = this.isTimewindowDisabled();
    if (this.onHistoryOnlyChanged()) {
      setTimeout(() => {
        this.notifyChanged();
      });
    } else {
      this.updateDisplayValue();
    }
  }

  notifyChanged() {
    this.propagateChange(cloneSelectedTimewindow(this.innerValue));
  }

  updateDisplayValue() {
    if (this.innerValue.selectedTab === TimewindowType.REALTIME && !this.historyOnly) {
      this.innerValue.displayValue = this.translate.instant('timewindow.realtime') + ' - ';
      if (this.innerValue.realtime.realtimeType === RealtimeWindowType.INTERVAL) {
        this.innerValue.displayValue += this.translate.instant(QuickTimeIntervalTranslationMap.get(this.innerValue.realtime.quickInterval));
      } else {
        this.innerValue.displayValue +=  this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.realtime.timewindowMs);
      }
    } else {
      this.innerValue.displayValue = (!this.historyOnly || this.alwaysDisplayTypePrefix) ? (this.translate.instant('timewindow.history') + ' - ') : '';
      if (this.innerValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        this.innerValue.displayValue += this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.history.timewindowMs);
      } else if (this.innerValue.history.historyType === HistoryWindowType.INTERVAL) {
        this.innerValue.displayValue += this.translate.instant(QuickTimeIntervalTranslationMap.get(this.innerValue.history.quickInterval));
      } else {
        const startString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.startTimeMs, 'yyyy-MM-dd HH:mm:ss');
        const endString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.endTimeMs, 'yyyy-MM-dd HH:mm:ss');
        this.innerValue.displayValue += this.translate.instant('timewindow.period', {startTime: startString, endTime: endString});
      }
    }
    if (isDefinedAndNotNull(this.innerValue.timezone) && this.innerValue.timezone !== '') {
      this.innerValue.displayValue += ' ';
      this.innerValue.displayTimezoneAbbr = getTimezoneInfo(this.innerValue.timezone).abbr;
    } else {
      this.innerValue.displayTimezoneAbbr = '';
    }
    this.cd.detectChanges();
  }

  hideLabel() {
    return this.isToolbar && !this.breakpointObserver.isMatched(MediaBreakpoints['gt-md']);
  }

  private isTimewindowDisabled(): boolean {
    return this.disabled ||
      (!this.isEdit && (!this.innerValue || this.innerValue.hideInterval &&
        (!this.aggregation || this.innerValue.hideAggregation && this.innerValue.hideAggInterval)));
  }

}
