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

import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { TooltipPosition } from '@angular/material/tooltip';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimezonePanelComponent, TimezoneSelectionResult } from '@shared/components/time/timezone-panel.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { getTimezoneInfo, TimezoneInfo } from '@shared/models/time/time.models';
import { TimeService } from '@core/services/time.service';

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
export class TimezoneComponent implements ControlValueAccessor, OnInit {

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

  @Input()
  tooltipPosition: TooltipPosition = 'above';

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
  }
  defaultTimezoneId: string = null;

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

  modelValue: string | null;
  timezoneInfo: TimezoneInfo;

  private localBrowserTimezoneInfoPlaceholder: TimezoneInfo = this.timeService.getLocalBrowserTimezoneInfoPlaceholder();

  timezoneDisabled: boolean;

  private propagateChange = (_: any) => {};

  constructor(private translate: TranslateService,
              private cd: ChangeDetectorRef,
              public viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private timeService: TimeService) {
  }

  ngOnInit() {
  }

  toggleTimezone($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.disablePanel) {
      return;
    }
    const trigger = ($event.target || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const timezoneSelectionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: TimezonePanelComponent,
        preferredPlacement: ['bottomRight', 'leftBottom'],
        context: {
          timezone: this.modelValue,
          userTimezoneByDefault: this.userTimezoneByDefaultValue,
          localBrowserTimezonePlaceholderOnEmpty: this.localBrowserTimezonePlaceholderOnEmptyValue,
          defaultTimezone: this.defaultTimezoneId,
          onClose: (result: TimezoneSelectionResult | null) => {
            timezoneSelectionPopover.hide();
            if (result) {
              this.modelValue = result.timezone;
              this.setTimezoneInfo();
              this.timezoneDisabled = this.isTimezoneDisabled();
              this.updateDisplayValue();
              this.notifyChanged();
            }
          }
        },
        showCloseButton: false,
        isModal: true
      });
      timezoneSelectionPopover.tbComponentRef.instance.popoverComponent = timezoneSelectionPopover;
    }
    this.cd.detectChanges();
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

  writeValue(value: string | null): void {
    this.modelValue = value;
    this.setTimezoneInfo();
    this.timezoneDisabled = this.isTimezoneDisabled();
    this.updateDisplayValue();
  }

  notifyChanged() {
    this.propagateChange(this.modelValue);
  }

  displayValue(): string {
    return this.displayTimezoneValue && this.timezoneInfo ? this.timezoneInfo.offset : this.translate.instant('timezone.timezone');
  }

  tooltipValue(): string {
    return this.timezoneInfo ? `${this.timezoneInfo.name} (${this.timezoneInfo.offset})` : undefined;
  }

  updateDisplayValue() {
    this.cd.detectChanges();
  }

  private isTimezoneDisabled(): boolean {
    return this.disabled;
  }

  private setTimezoneInfo() {
    const foundTimezone = getTimezoneInfo(this.modelValue, this.defaultTimezoneId, this.userTimezoneByDefaultValue);
    if (foundTimezone !== null) {
      this.timezoneInfo = foundTimezone;
    } else {
      if (this.localBrowserTimezonePlaceholderOnEmptyValue) {
        this.timezoneInfo = this.localBrowserTimezoneInfoPlaceholder;
      } else {
        this.timezoneInfo = null;
      }
    }
  }

}
