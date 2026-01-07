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

import { Component, forwardRef, OnDestroy } from '@angular/core';
import { Color, ColorPickerControl } from '@iplab/ngx-color-picker';
import { Subscription } from 'rxjs';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormControl } from '@angular/forms';
import { isString } from '@core/utils';

export enum ColorType {
  hex = 'hex',
  hexa = 'hexa',
  rgba = 'rgba',
  rgb = 'rgb',
  hsla = 'hsla',
  hsl = 'hsl',
  cmyk = 'cmyk'
}

const colorPresetsHex =
  ['#435B63', '#F44336', '#E89623', '#F5DD00', '#8BC34A', '#4CAF50', '#009688', '#048AD3', '#673AB7', '#9C27B0', '#E91E63', '#6F113A',
   '#A1ADB1', '#F9A19B', '#FFD190', '#FFF59D', '#C5E1A4', '#A5D7A7', '#80CBC3', '#81C4E9', '#B39CDB', '#CD93D7', '#F48FB1', '#BC91A4'];

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
export class ColorPickerComponent implements ControlValueAccessor, OnDestroy {

  presentations = [ColorType.hex, ColorType.rgba, ColorType.hsla];
  control = new ColorPickerControl();

  presentationControl = new UntypedFormControl(0);

  colorPresets: Color[] = colorPresetsHex.map(c => Color.from(c));

  private modelValue: string;

  private subscriptions: Array<Subscription> = [];

  private propagateChange = (v: any) => {};

  private setValue = false;

  constructor() {
    this.subscriptions.push(
      this.control.valueChanges.subscribe(() => {
        if (!this.setValue) {
          this.updateModel();
        } else {
          this.setValue = false;
        }
      })
    );
    this.subscriptions.push(
      this.presentationControl.valueChanges.subscribe(() => {
        this.updateModel();
      })
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string): void {
    const valid = this.isValidColorValue(value);
    this.setValue = valid;
    this.control.setValueFrom(valid ? value : '#fff');
    this.modelValue = value;

    if (this.control.initType === ColorType.hexa) {
      this.control.initType = ColorType.hex;
    } else if (this.control.initType === ColorType.rgb) {
      this.control.initType = ColorType.rgba;
    } else if (this.control.initType === ColorType.hsl) {
      this.control.initType = ColorType.hsla;
    }
    this.presentationControl.patchValue(this.presentations.indexOf(this.control.initType), {emitEvent: false});
  }

  private isValidColorValue(value: string): boolean {
    return value && isString(value) && value.trim().length > 0;
  }

  private updateModel() {
    const color: string = this.getValueByType(this.control.value, this.presentations[this.presentationControl.value]);
    if (this.modelValue !== color) {
      this.modelValue = color;
      this.propagateChange(color);
    }
  }

  public ngOnDestroy(): void {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.length = 0;
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
