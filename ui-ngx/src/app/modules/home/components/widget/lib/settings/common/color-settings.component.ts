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
  Component,
  forwardRef,
  Injectable,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ColorSettings, ColorType, ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  ColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/color-settings-panel.component';

@Injectable()
export class ColorSettingsComponentService {

  private colorSettingsComponents = new Set<ColorSettingsComponent>();

  constructor() {}

  public registerColorSettingsComponent(comp: ColorSettingsComponent) {
    this.colorSettingsComponents.add(comp);
  }

  public unregisterColorSettingsComponent(comp: ColorSettingsComponent) {
    this.colorSettingsComponents.delete(comp);
  }

  public getOtherColorSettingsComponents(comp: ColorSettingsComponent): ColorSettingsComponent[] {
    const result: ColorSettingsComponent[] = [];
    for (const component of this.colorSettingsComponents.values()) {
      if (component.settingsKey && component.modelValue && component !== comp) {
        result.push(component);
      }
    }
    return result;
  }

}

@Component({
  selector: 'tb-color-settings',
  templateUrl: './color-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorSettingsComponent),
      multi: true
    }
  ]
})
export class ColorSettingsComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  settingsKey: string;

  colorType = ColorType;

  modelValue: ColorSettings;

  colorStyle: ComponentStyle = {};

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private colorSettingsComponentService: ColorSettingsComponentService) {}

  ngOnInit(): void {
    this.colorSettingsComponentService.registerColorSettingsComponent(this);
  }

  ngOnDestroy() {
    this.colorSettingsComponentService.unregisterColorSettingsComponent(this);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateColorStyle();
  }

  writeValue(value: ColorSettings): void {
    this.modelValue = value;
    this.updateColorStyle();
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
        settingsComponents: this.colorSettingsComponentService.getOtherColorSettingsComponents(this)
      };
      const colorSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ColorSettingsPanelComponent, 'left', true, null,
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
      let colors: string[] = [this.modelValue.color];
      if (this.modelValue.type === ColorType.range && this.modelValue.rangeList?.length) {
        const rangeColors = this.modelValue.rangeList.slice(0, Math.min(2, this.modelValue.rangeList.length)).map(r => r.color);
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
