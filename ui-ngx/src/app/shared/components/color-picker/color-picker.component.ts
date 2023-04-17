///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import { Color, ColorPickerControl } from '@iplab/ngx-color-picker';
import { Subscription } from 'rxjs';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export enum ColorType {
  hex = 'hex',
  hexa = 'hexa',
  rgba = 'rgba',
  rgb = 'rgb',
  hsla = 'hsla',
  hsl = 'hsl',
  cmyk = 'cmyk'
}

@Component({
  selector: `tb-color-picker`,
  templateUrl: `./color-picker.component.html`,
  styleUrls: [`./color-picker.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorPickerComponent),
      multi: true
    }
  ]
})
export class ColorPickerComponent implements OnInit, ControlValueAccessor, OnDestroy {

  selectedPresentation = 0;
  presentations = [ColorType.hex, ColorType.rgba, ColorType.hsla];
  control = new ColorPickerControl();

  private modelValue: string;

  private subscriptions: Array<Subscription> = [];

  private propagateChange = null;

  constructor() {
  }

  public ngOnInit(): void {
    this.subscriptions.push(
      this.control.valueChanges.subscribe(value => {
        if (this.modelValue) {
          this.updateModel();
        }
      })
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string): void {
    this.control. setValueFrom(value || '#fff');
    this.modelValue = value;

    if (this.control.initType === ColorType.hexa) {
      this.control.initType = ColorType.hex;
    } else if (this.control.initType === ColorType.rgb) {
      this.control.initType = ColorType.rgba;
    } else if (this.control.initType === ColorType.hsl) {
      this.control.initType = ColorType.hsla;
    }

    this.selectedPresentation = this.presentations.indexOf(this.control.initType);
  }

  private updateModel() {
    const color: string = this.getValueByType(this.control.value, this.presentations[this.selectedPresentation]);
    if (this.modelValue !== color) {
      this.modelValue = color;
      this.propagateChange(color);
    }
  }

  public ngOnDestroy(): void {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.length = 0;
  }

  public changePresentation(): void {
    this.selectedPresentation =
      this.selectedPresentation === this.presentations.length - 1 ? 0 : this.selectedPresentation + 1;
    this.updateModel();
  }

  getValueByType(color: Color, type: ColorType): string {
    switch (type) {
      case ColorType.hex:
        return color.toHexString(this.control.value.getRgba().getAlpha() !== 1);
      case ColorType.rgba:
        return this.control.value.getRgba().getAlpha() !== 1 ? color.toRgbaString() : color.toRgbString();
      case ColorType.hsla:
        return this.control.value.getRgba().getAlpha() !== 1 ? color.toHslaString() : color.toHslString();
      default:
        return color.toRgbaString();
    }
  }
}
