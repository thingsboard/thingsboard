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
import { ColorRange, ColorSettings, ColorType, ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  ColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/color-settings-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';

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

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  @coerceBoolean()
  rangeAdvancedMode = false;

  @Input()
  @coerceBoolean()
  gradientAdvancedMode = false;

  @Input()
  minValue: number;

  @Input()
  maxValue: number;

  colorType = ColorType;

  modelValue: ColorSettings;

  colorStyle: ComponentStyle = {};

  private propagateChange: (v: any) => void = () => { };

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
    if (value) {
      this.modelValue = value;
      if (isDefinedAndNotNull(this.modelValue.rangeList) && !isDefinedAndNotNull(this.modelValue.rangeList?.advancedMode)) {
        const range = deepClone(this.modelValue.rangeList) as ColorRange[];
        this.modelValue.rangeList = deepClone({advancedMode: false, range});
      }
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
        componentType: ColorSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          colorSettings: this.modelValue,
          settingsComponents: this.colorSettingsComponentService.getOtherColorSettingsComponents(this),
          aliasController: this.aliasController,
          dataKeyCallbacks: this.dataKeyCallbacks,
          datasource: this.datasource,
          rangeAdvancedMode: this.rangeAdvancedMode,
          gradientAdvancedMode: this.gradientAdvancedMode,
          minValue: this.minValue,
          maxValue: this.maxValue
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
    if (!this.disabled && this.modelValue) {
      let colors: string[] = [this.modelValue.color];
      const rangeList = this.modelValue.rangeList;
      if (this.modelValue.type === ColorType.range && (rangeList?.range?.length || rangeList?.rangeAdvanced?.length)) {
        let rangeColors: Array<string>;
        if (rangeList?.advancedMode) {
          rangeColors = rangeList.rangeAdvanced.slice(0, Math.min(2, rangeList.rangeAdvanced.length)).map(r => r.color);
        } else {
          rangeColors = rangeList.range.slice(0, Math.min(2, rangeList.range.length)).map(r => r.color);
        }
        colors = colors.concat(rangeColors);
      } else if (this.modelValue.type === ColorType.gradient) {
        colors = this.modelValue.gradient?.advancedMode ?
          this.modelValue.gradient.gradientAdvanced.map(color => color.color) :
          this.modelValue.gradient.gradient;
      }
      if (colors.length === 1) {
        this.colorStyle = {backgroundColor: colors[0]};
      } else {
        const gradientValues: string[] = [];
        if (this.modelValue.type === ColorType.gradient) {
          gradientValues.push(...colors);
        } else {
          const step = 100 / colors.length;
          for (let i = 0; i < colors.length; i++) {
            gradientValues.push(`${colors[i]} ${step*i}%`);
            gradientValues.push(`${colors[i]} ${step*(i+1)}%`);
          }
        }
        this.colorStyle = {background: `linear-gradient(90deg, ${gradientValues.join(', ')})`};
      }
    } else {
      this.colorStyle = {};
    }
  }

}
