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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimewindowStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { Timewindow } from '@shared/models/time/time.models';
import { TimewindowStylePanelComponent } from '@home/components/widget/config/timewindow-style-panel.component';

@Component({
    selector: 'tb-timewindow-style',
    templateUrl: './timewindow-style.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TimewindowStyleComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TimewindowStyleComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  previewValue: Timewindow;

  private modelValue: TimewindowStyle;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: TimewindowStyle): void {
    this.modelValue = value;
  }

  openTimewindowStylePopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        timewindowStyle: this.modelValue,
        previewValue: this.previewValue
      };
      const timewindowStylePanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: TimewindowStylePanelComponent,
        preferredPlacement: 'left',
        context: ctx,
        isModal: true
      });
      timewindowStylePanelPopover.tbComponentRef.instance.popover = timewindowStylePanelPopover;
      timewindowStylePanelPopover.tbComponentRef.instance.timewindowStyleApplied.subscribe((timewindowStyle) => {
        timewindowStylePanelPopover.hide();
        this.modelValue = timewindowStyle;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
