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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  HostBinding,
  Injector,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  StaticProvider,
  ViewChild,
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
import {
  TIMEWINDOW_PANEL_DATA,
  TimewindowPanelComponent,
  TimewindowPanelData
} from '@shared/components/time/timewindow-panel.component';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  ComponentStyle,
  defaultTimewindowStyle,
  iconStyle,
  textStyle,
  TimewindowStyle
} from '@shared/models/widget-settings.models';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { fromEvent } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
export class TimewindowComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('panelContainer', { read: ViewContainerRef, static: true }) panelContainer: ViewContainerRef;

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

  get displayTypePrefix(): boolean {
    return isDefinedAndNotNull(this.computedTimewindowStyle?.displayTypePrefix)
      ? this.computedTimewindowStyle?.displayTypePrefix : true;
  }

  @HostBinding('class.no-margin')
  @Input()
  @coerceBoolean()
  noMargin = false;

  @Input()
  @coerceBoolean()
  noPadding = false;

  @Input()
  @coerceBoolean()
  disablePanel = false;

  @Input()
  @coerceBoolean()
  forAllTimeEnabled = false;

  @Input()
  @coerceBoolean()
  alwaysDisplayTypePrefix = false;

  @Input()
  @coerceBoolean()
  quickIntervalOnly = false;

  @Input()
  @coerceBoolean()
  aggregation = false;

  @Input()
  @coerceBoolean()
  timezone = false;

  @Input()
  @coerceBoolean()
  isToolbar = false;

  @Input()
  @coerceBoolean()
  asButton = false;

  @Input()
  @coerceBoolean()
  strokedButton = false;

  @Input()
  @coerceBoolean()
  flatButton = false;

  @Input()
  @coerceBoolean()
  displayTimewindowValue = true;

  @Input()
  @coerceBoolean()
  hideLabel = false;

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
  tooltipPosition: TooltipPosition = 'above';

  @Input()
  timewindowStyle: TimewindowStyle;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  panelMode = true;

  innerValue: Timewindow;

  timewindowDisabled: boolean;

  computedTimewindowStyle: TimewindowStyle;
  timewindowComponentStyle: ComponentStyle;
  timewindowIconStyle: ComponentStyle;

  private propagateChange = (_: any) => {};

  constructor(private overlay: Overlay,
              private translate: TranslateService,
              private timeService: TimeService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private cd: ChangeDetectorRef,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.updateTimewindowStyle();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'timewindowStyle') {
          this.updateTimewindowStyle();
          this.updateDisplayValue();
        }
      }
    }
  }

  toggleTimewindow($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.disablePanel || this.timewindowDisabled) {
      return;
    }
    const config = new OverlayConfig({
      panelClass: 'tb-timewindow-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '70vh',
      height: 'min-content'
    });

    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions(DEFAULT_OVERLAY_POSITIONS);

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
          forAllTimeEnabled: this.forAllTimeEnabled,
          quickIntervalOnly: this.quickIntervalOnly,
          aggregation: this.aggregation,
          timezone: this.timezone,
          isEdit: this.isEdit,
          panelMode: this.panelMode,
        } as TimewindowPanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(TimewindowPanelComponent,
      this.viewContainerRef, injector));
    const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
      overlayRef.updatePosition();
    });
    componentRef.onDestroy(() => {
      resizeWindows$.unsubscribe();
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.timewindowDisabled = this.isTimewindowDisabled();
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
    this.cd.detectChanges();
  }

  private updateTimewindowStyle() {
    if (!this.asButton) {
      this.computedTimewindowStyle = {...defaultTimewindowStyle, ...(this.timewindowStyle || {})};
      this.timewindowComponentStyle = textStyle(this.computedTimewindowStyle.font);
      if (this.computedTimewindowStyle.color) {
        this.timewindowComponentStyle.color = this.computedTimewindowStyle.color;
      }
      this.timewindowIconStyle = this.computedTimewindowStyle.iconSize ? iconStyle(this.computedTimewindowStyle.iconSize) : {};
    }
  }

  private onHistoryOnlyChanged(): boolean {
    if (this.historyOnlyValue && this.innerValue && this.innerValue.selectedTab !== TimewindowType.HISTORY) {
      this.innerValue.selectedTab = TimewindowType.HISTORY;
      this.updateDisplayValue();
      return true;
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
    this.innerValue = initModelFromDefaultTimewindow(obj, this.quickIntervalOnly, this.historyOnly, this.timeService,
      this.aggregation);
    this.timewindowDisabled = this.isTimewindowDisabled();
    if (this.onHistoryOnlyChanged()) {
      setTimeout(() => {
        this.notifyChanged();
      });
    } else {
      this.updateDisplayValue();
    }
    if (!this.panelMode) {
      this.createPanel();
    }
  }

  notifyChanged() {
    this.propagateChange(cloneSelectedTimewindow(this.innerValue));
  }

  displayValue(): string {
    return this.displayTimewindowValue ? this.innerValue?.displayValue : this.translate.instant('timewindow.timewindow');
  }

  updateDisplayValue() {
    if (!this.panelMode) {
      return
    }
    if (this.innerValue.selectedTab === TimewindowType.REALTIME && !this.historyOnly) {
      this.innerValue.displayValue = this.displayTypePrefix ? (this.translate.instant('timewindow.realtime') + ' - ') : '';
      if (this.innerValue.realtime.realtimeType === RealtimeWindowType.INTERVAL) {
        this.innerValue.displayValue += this.translate.instant(QuickTimeIntervalTranslationMap.get(this.innerValue.realtime.quickInterval));
      } else {
        this.innerValue.displayValue +=  this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.realtime.timewindowMs);
      }
    } else {
      this.innerValue.displayValue = this.displayTypePrefix && (!this.historyOnly || this.alwaysDisplayTypePrefix) ?
        (this.translate.instant('timewindow.history') + ' - ') : '';
      if (this.innerValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        this.innerValue.displayValue += this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.history.timewindowMs);
      } else if (this.innerValue.history.historyType === HistoryWindowType.INTERVAL) {
        this.innerValue.displayValue += this.translate.instant(QuickTimeIntervalTranslationMap.get(this.innerValue.history.quickInterval));
      } else if (this.innerValue.history.historyType === HistoryWindowType.FOR_ALL_TIME) {
        this.innerValue.displayValue += this.translate.instant('timewindow.for-all-time');
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

  private isTimewindowDisabled(): boolean {
    return this.disabled ||
      (!this.isEdit && (!this.innerValue || (
        ((this.innerValue.realtime?.hideInterval && this.innerValue.history?.hideInterval) ||
          (this.innerValue.realtime?.hideLastInterval && this.innerValue.realtime?.hideQuickInterval &&
            this.innerValue.history?.hideLastInterval && this.innerValue.history?.hideFixedInterval &&
            this.innerValue.history?.hideQuickInterval)) &&
        (!this.aggregation || this.innerValue.hideAggregation && this.innerValue.hideAggInterval) &&
        (!this.timezone || this.innerValue.hideTimezone)
      )));
  }

  private createPanel() {
    this.panelContainer.clear();
    const panelData = {
      timewindow: deepClone(this.innerValue),
      historyOnly: this.historyOnly,
      forAllTimeEnabled: this.forAllTimeEnabled,
      quickIntervalOnly: this.quickIntervalOnly,
      aggregation: this.aggregation,
      timezone: this.timezone,
      isEdit: this.isEdit,
      panelMode: this.panelMode,
    }
    const injector = Injector.create({
      providers: [{ provide: TIMEWINDOW_PANEL_DATA, useValue: panelData }],
      parent: this.viewContainerRef.injector
    });
    const componentRef = this.panelContainer.createComponent(TimewindowPanelComponent, {index: 0, injector});
    componentRef.instance.changeTimewindow.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.innerValue = value;
      this.timewindowDisabled = this.isTimewindowDisabled();
      this.notifyChanged();
    })
  }
}
