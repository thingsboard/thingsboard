///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  forwardRef,
  Inject,
  Injector,
  Input,
  OnDestroy,
  OnInit,
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
import { DatePipe, DOCUMENT } from '@angular/common';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  TIMEWINDOW_PANEL_DATA,
  TimewindowPanelComponent,
  TimewindowPanelData
} from '@shared/components/time/timewindow-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { WINDOW } from '@core/services/window.service';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

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
    this.historyOnlyValue = coerceBooleanProperty(val);
  }

  get historyOnly() {
    return this.historyOnlyValue;
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

  @ViewChild('timewindowPanelOrigin') timewindowPanelOrigin: CdkOverlayOrigin;

  innerValue: Timewindow;

  timewindowDisabled: boolean;

  private propagateChange = (_: any) => {};

  constructor(private translate: TranslateService,
              private timeService: TimeService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private overlay: Overlay,
              public viewContainerRef: ViewContainerRef,
              public breakpointObserver: BreakpointObserver,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  openEditMode() {
    if (this.timewindowDisabled) {
      return;
    }
    const isGtXs = this.breakpointObserver.isMatched(MediaBreakpoints['gt-xs']);
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-timewindow-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: isGtXs,
    });
    if (isGtXs) {
      config.minWidth = '417px';
      config.maxHeight = '500px';
      const panelHeight = 375;
      const panelWidth = 417;
      const el = this.timewindowPanelOrigin.elementRef.nativeElement;
      const offset = el.getBoundingClientRect();
      const scrollTop = this.window.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0;
      const scrollLeft = this.window.pageXOffset || this.document.documentElement.scrollLeft || this.document.body.scrollLeft || 0;
      const bottomY = offset.bottom - scrollTop;
      const leftX = offset.left - scrollLeft;
      let originX;
      let originY;
      let overlayX;
      let overlayY;
      const wHeight = this.document.documentElement.clientHeight;
      const wWidth = this.document.documentElement.clientWidth;
      if (bottomY + panelHeight > wHeight) {
        originY = 'top';
        overlayY = 'bottom';
      } else {
        originY = 'bottom';
        overlayY = 'top';
      }
      if (leftX + panelWidth > wWidth) {
        originX = 'end';
        overlayX = 'end';
      } else {
        originX = 'start';
        overlayX = 'start';
      }
      const connectedPosition: ConnectedPosition = {
        originX,
        originY,
        overlayX,
        overlayY
      };
      config.positionStrategy = position.flexibleConnectedTo(this.timewindowPanelOrigin.elementRef)
        .withPositions([connectedPosition]);
    } else {
      config.minWidth = '100%';
      config.minHeight = '100%';
      config.positionStrategy = position.global().top('0%').left('0%')
        .right('0%').bottom('0%');
    }

    const overlayRef = this.overlay.create(config);

    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createTimewindowPanelInjector(
      overlayRef,
      {
        timewindow: deepClone(this.innerValue),
        historyOnly: this.historyOnly,
        aggregation: this.aggregation,
        timezone: this.timezone,
        isEdit: this.isEdit
      }
    );

    const componentRef = overlayRef.attach(new ComponentPortal(TimewindowPanelComponent, this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.timewindowDisabled = this.isTimewindowDisabled();
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
  }

  private _createTimewindowPanelInjector(overlayRef: OverlayRef, data: TimewindowPanelData): Injector {
    const providers: StaticProvider[] = [
      {provide: TIMEWINDOW_PANEL_DATA, useValue: data},
      {provide: OverlayRef, useValue: overlayRef}
    ];
    return Injector.create({parent: this.viewContainerRef.injector, providers});
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
    this.innerValue = initModelFromDefaultTimewindow(obj, this.timeService);
    this.timewindowDisabled = this.isTimewindowDisabled();
    this.updateDisplayValue();
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
      this.innerValue.displayValue = !this.historyOnly ? (this.translate.instant('timewindow.history') + ' - ') : '';
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
