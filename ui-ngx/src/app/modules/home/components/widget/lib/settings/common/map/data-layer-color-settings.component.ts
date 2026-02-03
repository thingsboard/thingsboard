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

import { Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ColorType, ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { DataLayerColorSettings, DataLayerColorType } from '@shared/models/widget/maps/map.models';
import {
  DataLayerColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/data-layer-color-settings-panel.component';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';

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
    ],
    standalone: false
})
export class DataLayerColorSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Input()
  dsType: DatasourceType;

  @Input()
  dsEntityAliasId: string;

  @Input()
  dsDeviceId: string;

  @Input()
  helpId = 'widget/lib/map/color_fn';

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
      const colorSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: DataLayerColorSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          colorSettings: this.modelValue,
          context: this.context,
          dsType: this.dsType,
          dsEntityAliasId: this.dsEntityAliasId,
          dsDeviceId: this.dsDeviceId,
          helpId: this.helpId
        },
        isModal: true
      });
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
    if (!this.disabled && this.modelValue && this.modelValue.type !== DataLayerColorType.function) {
      let colors: string[] = [this.modelValue.color];
      const rangeList = this.modelValue.range;
      if (this.modelValue.type === DataLayerColorType.range && rangeList?.length) {
        const rangeColors = rangeList.slice(0, Math.min(2, rangeList.length)).map(r => r.color);
        colors = colors.concat(rangeColors);
      }
      if (colors.length === 1) {
        this.colorStyle = {backgroundColor: colors[0]};
      } else {
        const gradientValues: string[] = [];
        const step = 100 / colors.length;
        for (let i = 0; i < colors.length; i++) {
          gradientValues.push(`${colors[i]} ${step*i}%`);
          gradientValues.push(`${colors[i]} ${step*(i+1)}%`);
        }
        this.colorStyle = {background: `linear-gradient(90deg, ${gradientValues.join(', ')})`};
      }
    } else {
      this.colorStyle = {};
    }
  }

}
