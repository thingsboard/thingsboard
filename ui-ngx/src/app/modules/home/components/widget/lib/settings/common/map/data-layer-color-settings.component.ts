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

import { Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { DataLayerColorSettings, DataLayerColorType } from '@home/components/widget/lib/maps/map.models';
import {
  DataLayerColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/data-layer-color-settings-panel.component';

@Component({
  selector: 'tb-data-layer-color-settings',
  templateUrl: './data-layer-color-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataLayerColorSettingsComponent),
      multi: true
    }
  ]
})
export class DataLayerColorSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  DataLayerColorType = DataLayerColorType;

  modelValue: DataLayerColorSettings;

  colorStyle: ComponentStyle = {};

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateColorStyle();
  }

  writeValue(value: DataLayerColorSettings): void {
    if (value) {
      this.modelValue = value;
      this.updateColorStyle();
    }
  }

  openColorSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        colorSettings: this.modelValue,
      };
      const colorSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DataLayerColorSettingsPanelComponent, 'left', true, null,
        ctx,
        {},
        {}, {}, true);
      colorSettingsPanelPopover.tbComponentRef.instance.popover = colorSettingsPanelPopover;
      colorSettingsPanelPopover.tbComponentRef.instance.colorSettingsApplied.subscribe((colorSettings) => {
        colorSettingsPanelPopover.hide();
        this.modelValue = colorSettings;
        this.updateColorStyle();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateColorStyle() {
    if (!this.disabled && this.modelValue) {
      if (this.modelValue.type === DataLayerColorType.constant) {
        this.colorStyle = {backgroundColor: this.modelValue.color};
      } else {
        this.colorStyle = {};
      }
    } else {
      this.colorStyle = {};
    }
  }

}
