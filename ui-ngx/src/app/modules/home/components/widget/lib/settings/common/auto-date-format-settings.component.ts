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
import { AutoDateFormatSettings, defaultAutoDateFormatSettings } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone, mergeDeep } from '@core/utils';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';

@Component({
    selector: 'tb-auto-date-format-settings',
    templateUrl: './auto-date-format-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AutoDateFormatSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AutoDateFormatSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  defaultValues = defaultAutoDateFormatSettings;

  private modelValue: AutoDateFormatSettings;

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

  writeValue(value: AutoDateFormatSettings): void {
    this.modelValue = value;
  }

  openAutoFormatSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const autoDateFormatSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: AutoDateFormatSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          autoDateFormatSettings: deepClone(this.modelValue),
          defaultValues: this.defaultValues
        },
        isModal: true
      });
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.popover = autoDateFormatSettingsPanelPopover;
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.autoDateFormatSettingsApplied.subscribe((autoDateFormatSettings) => {
        autoDateFormatSettingsPanelPopover.hide();
        this.modelValue = autoDateFormatSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
