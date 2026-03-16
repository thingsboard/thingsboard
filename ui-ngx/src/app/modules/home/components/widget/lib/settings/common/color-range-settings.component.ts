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
import { ColorRange, ColorRangeSettings, ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ColorRangePanelComponent } from '@home/components/widget/lib/settings/common/color-range-panel.component';

@Injectable()
export class ColorRangeSettingsComponentService {

  private colorSettingsComponents = new Set<ColorRangeSettingsComponent>();

  constructor() {}

  public registerColorSettingsComponent(comp: ColorRangeSettingsComponent) {
    this.colorSettingsComponents.add(comp);
  }

  public unregisterColorSettingsComponent(comp: ColorRangeSettingsComponent) {
    this.colorSettingsComponents.delete(comp);
  }

  public getOtherColorSettingsComponents(comp: ColorRangeSettingsComponent): ColorRangeSettingsComponent[] {
    const result: ColorRangeSettingsComponent[] = [];
    for (const component of this.colorSettingsComponents.values()) {
      if (component.settingsKey && component.modelValue && component !== comp) {
        result.push(component);
      }
    }
    return result;
  }

}

@Component({
    selector: 'tb-color-range-settings',
    templateUrl: './color-range-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ColorRangeSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ColorRangeSettingsComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  settingsKey: string;

  modelValue: Array<ColorRange>;

  colorStyle: ComponentStyle = {};

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private colorSettingsComponentService: ColorRangeSettingsComponentService) {}

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

  writeValue(value: Array<ColorRange> | ColorRangeSettings): void {
    this.modelValue = Array.isArray(value) ? value : value.range;
    this.updateColorStyle();
  }

  openColorRangeSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const colorRangeSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ColorRangePanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          colorRangeSettings: this.modelValue,
          settingsComponents: this.colorSettingsComponentService.getOtherColorSettingsComponents(this)
        },
        isModal: true
      });
      colorRangeSettingsPanelPopover.tbComponentRef.instance.popover = colorRangeSettingsPanelPopover;
      colorRangeSettingsPanelPopover.tbComponentRef.instance.colorRangeApplied.subscribe((colorRangeSettings: Array<ColorRange>) => {
        colorRangeSettingsPanelPopover.hide();
        this.modelValue = colorRangeSettings;
        this.updateColorStyle();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateColorStyle() {
    if (!this.disabled && this.modelValue) {
      let colors: string[] = [];
      if (this.modelValue.length) {
        const rangeColors = this.modelValue.slice(0, Math.min(3, this.modelValue.length)).map(r => r.color);
        colors = colors.concat(rangeColors);
      }
      if (!colors.length) {
        this.colorStyle = {};
      } else if (colors.length === 1) {
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
