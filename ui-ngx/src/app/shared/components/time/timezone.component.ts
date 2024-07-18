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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  HostBinding,
  Injector,
  Input,
  OnChanges,
  OnInit, Renderer2,
  SimpleChanges,
  StaticProvider,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { DatePipe } from '@angular/common';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  ComponentStyle,
  defaultTimezoneStyle,
  iconStyle,
  textStyle,
  TimezoneStyle
} from '@shared/models/widget-settings.models';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { fromEvent } from 'rxjs';
import {
  TIMEZONE_PANEL_DATA,
  TimezonePanelComponent,
  TimezonePanelData
} from '@shared/components/time/timezone-panel.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { TimewindowStylePanelComponent } from '@home/components/widget/config/timewindow-style-panel.component';

// @dynamic
@Component({
  selector: 'tb-timezone',
  templateUrl: './timezone.component.html',
  styleUrls: ['./timezone.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimezoneComponent),
      multi: true
    }
  ]
})
export class TimezoneComponent implements ControlValueAccessor, OnInit, OnChanges {

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
  displayTimezoneValue = true;

  @Input()
  @coerceBoolean()
  hideLabel = false;

  isEditValue = false;

  @Input()
  set isEdit(val) {
    this.isEditValue = coerceBooleanProperty(val);
    this.timezoneDisabled = this.isTimezoneDisabled();
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input()
  timezoneStyle: TimezoneStyle;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  private userTimezoneByDefaultValue: boolean;
  get userTimezoneByDefault(): boolean {
    return this.userTimezoneByDefaultValue;
  }
  @Input()
  set userTimezoneByDefault(value: boolean) {
    this.userTimezoneByDefaultValue = coerceBooleanProperty(value);
  }

  private localBrowserTimezonePlaceholderOnEmptyValue: boolean;
  get localBrowserTimezonePlaceholderOnEmpty(): boolean {
    return this.localBrowserTimezonePlaceholderOnEmptyValue;
  }
  @Input()
  set localBrowserTimezonePlaceholderOnEmpty(value: boolean) {
    this.localBrowserTimezonePlaceholderOnEmptyValue = coerceBooleanProperty(value);
  }defaultTimezoneId: string = null;

  @Input()
  set defaultTimezone(timezone: string) {
    if (this.defaultTimezoneId !== timezone) {
      this.defaultTimezoneId = timezone;
    }
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  innerValue: string;

  timezoneDisabled: boolean;

  computedTimezoneStyle: TimezoneStyle;
  timezoneComponentStyle: ComponentStyle;
  timezoneIconStyle: ComponentStyle;

  private propagateChange = (_: any) => {};

  constructor(private overlay: Overlay,
              private translate: TranslateService,
              private timeService: TimeService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private cd: ChangeDetectorRef,
              private nativeElement: ElementRef,
              public viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2) {
  }

  ngOnInit() {
    this.updateTimezoneStyle();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'timezoneStyle') {
          this.updateTimezoneStyle();
          this.updateDisplayValue();
        }
      }
    }
  }

  toggleTimezone($event: Event) {
    console.log($event);
    if ($event) {
      $event.stopPropagation();
    }
    if (this.disablePanel) {
      return;
    }
    // const trigger = ($event.target || $event.srcElement || $event.currentTarget) as Element;
    // if (this.popoverService.hasPopover(trigger)) {
    //   this.popoverService.hidePopover(trigger);
    // } else {
    //   const timezoneSelectionPopover = this.popoverService.displayPopover(trigger, this.renderer,
    //     this.viewContainerRef, TimewindowStylePanelComponent, 'left', true, null,
    //     ctx,
    //     {},
    //     {}, {}, true);
    //   timewindowStylePanelPopover.tbComponentRef.instance.popover = timewindowStylePanelPopover;
    //   timewindowStylePanelPopover.tbComponentRef.instance.timewindowStyleApplied.subscribe((timewindowStyle) => {
    //     timewindowStylePanelPopover.hide();
    //     this.modelValue = timewindowStyle;
    //     this.propagateChange(this.modelValue);
    //   });
    //
    //   // if (componentRef.instance.result) {
    //   //   this.innerValue = componentRef.instance.result;
    //   //   this.timezoneDisabled = this.isTimezoneDisabled();
    //   //   this.updateDisplayValue();
    //   //   this.notifyChanged();
    //   // }
    // }
    const config = new OverlayConfig({
      panelClass: 'tb-timezone-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '30vh',
      height: 'min-content'
    });

    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions(DEFAULT_OVERLAY_POSITIONS);

    // TODO: change panel to popover
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: TIMEZONE_PANEL_DATA,
        useValue: {
          timezone: this.innerValue,
          isEdit: this.isEdit
        } as TimezonePanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(TimezonePanelComponent,
      this.viewContainerRef, injector));
    const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
      overlayRef.updatePosition();
    });
    componentRef.onDestroy(() => {
      resizeWindows$.unsubscribe();
      // TODO: if value check makes impossible to select browser timezone (null)
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.timezoneDisabled = this.isTimezoneDisabled();
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
    this.cd.detectChanges();
  }

  private updateTimezoneStyle() {
    if (!this.asButton) {
      this.computedTimezoneStyle = {...defaultTimezoneStyle, ...(this.timezoneStyle || {})};
      this.timezoneComponentStyle = textStyle(this.computedTimezoneStyle.font);
      if (this.computedTimezoneStyle.color) {
        this.timezoneComponentStyle.color = this.computedTimezoneStyle.color;
      }
      this.timezoneIconStyle = this.computedTimezoneStyle.iconSize ? iconStyle(this.computedTimezoneStyle.iconSize) : {};
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.timezoneDisabled = this.isTimezoneDisabled();
  }

  writeValue(value: string): void {
    this.innerValue = value;
    this.timezoneDisabled = this.isTimezoneDisabled();
    this.updateDisplayValue();
  }

  notifyChanged() {
    this.propagateChange(this.innerValue);
  }

  displayValue(): string {
    // TODO: only offset should be displayed, timezone name shouldn't be present
    return this.displayTimezoneValue ? this.innerValue : this.translate.instant('timezone.timezone');
  }

  updateDisplayValue() {
    this.cd.detectChanges();
  }

  private isTimezoneDisabled(): boolean {
    return this.disabled;
  }

}
