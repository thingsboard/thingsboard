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
  ChangeDetectionStrategy,
  Component,
  forwardRef, Inject,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import {
  HistoryWindowType,
  Timewindow,
  TimewindowType,
  initModelFromDefaultTimewindow, cloneSelectedTimewindow
} from '@shared/models/time/time.models';
import { DatePipe } from '@angular/common';
import {
  Overlay,
  CdkOverlayOrigin,
  OverlayConfig,
  OverlayPositionBuilder, ConnectedPosition, PositionStrategy, OverlayRef
} from '@angular/cdk/overlay';
import {
  TIMEWINDOW_PANEL_DATA,
  TimewindowPanelComponent,
  TimewindowPanelData
} from '@shared/components/time/timewindow-panel.component';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { deepClone } from '@core/utils';

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
    this.historyOnlyValue = true;
  }

  get historyOnly() {
    return this.historyOnlyValue;
  }

  aggregationValue = false;

  @Input()
  set aggregation(val) {
    this.aggregationValue = true;
  }

  get aggregation() {
    return this.aggregationValue;
  }

  isToolbarValue = false;

  @Input()
  set isToolbar(val) {
    this.isToolbarValue = true;
  }

  get isToolbar() {
    return this.isToolbarValue;
  }

  asButtonValue = false;

  @Input()
  set asButton(val) {
    this.asButtonValue = true;
  }

  get asButton() {
    return this.asButtonValue;
  }

  @Input()
  direction: 'left' | 'right' = 'left';

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  @ViewChild('timewindowPanelOrigin') timewindowPanelOrigin: CdkOverlayOrigin;

  innerValue: Timewindow;

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
    if (this.disabled) {
      return;
    }
    const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-timewindow-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: isGtSm,
    });
    if (isGtSm) {
      config.minWidth = '417px';
      config.maxHeight = '440px';
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
        aggregation: this.aggregation
      }
    );

    const componentRef = overlayRef.attach(new ComponentPortal(TimewindowPanelComponent, this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
  }

  private _createTimewindowPanelInjector(overlayRef: OverlayRef, data: TimewindowPanelData): PortalInjector {
    const injectionTokens = new WeakMap<any, any>([
      [TIMEWINDOW_PANEL_DATA, data],
      [OverlayRef, overlayRef]
    ]);
    return new PortalInjector(this.viewContainerRef.injector, injectionTokens);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: Timewindow): void {
    this.innerValue = initModelFromDefaultTimewindow(obj, this.timeService);
    this.updateDisplayValue();
  }

  notifyChanged() {
    this.propagateChange(cloneSelectedTimewindow(this.innerValue));
  }

  updateDisplayValue() {
    if (this.innerValue.selectedTab === TimewindowType.REALTIME && !this.historyOnly) {
      this.innerValue.displayValue = this.translate.instant('timewindow.realtime') + ' - ' +
        this.translate.instant('timewindow.last-prefix') + ' ' +
        this.millisecondsToTimeStringPipe.transform(this.innerValue.realtime.timewindowMs);
    } else {
      this.innerValue.displayValue = !this.historyOnly ? (this.translate.instant('timewindow.history') + ' - ') : '';
      if (this.innerValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        this.innerValue.displayValue += this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.history.timewindowMs);
      } else {
        const startString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.startTimeMs, 'yyyy-MM-dd HH:mm:ss');
        const endString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.endTimeMs, 'yyyy-MM-dd HH:mm:ss');
        this.innerValue.displayValue += this.translate.instant('timewindow.period', {startTime: startString, endTime: endString});
      }
    }
  }

  hideLabel() {
    return this.isToolbar && !this.breakpointObserver.isMatched(MediaBreakpoints['gt-md']);
  }

}
