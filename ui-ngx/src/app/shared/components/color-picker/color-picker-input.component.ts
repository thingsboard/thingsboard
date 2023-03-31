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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';
import { ColorType } from '@shared/components/color-picker/color-picker.component';

@Component({
  selector: `tb-color-picker-input-component`,
  templateUrl: `./color-picker-input.component.html`,
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ColorPickerInputComponent {

  @Input()
  public hue: Color;

  @Output()
  public hueChange = new EventEmitter<Color>(false);

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  public labelVisible: boolean;

  @Input()
  public set label(value) {
    this.labelVisible = true;
  }

  @Input()
  public colorFormat: string;

  public isAlphaVisible = true;

  @Input()
  public set alpha(isVisible: boolean) {
    this.isAlphaVisible = isVisible;
  }

  public get value() {
    return this.color ? this.color.getRgba() : null;
  }

  constructor() {
  }

  public onInputChange(newValue, color: 'R' | 'G' | 'B' | 'A') {
    const value = this.value;
    const red = color === 'R' ? newValue : value.red;
    const green = color === 'G' ? newValue : value.green;
    const blue = color === 'B' ? newValue : value.blue;
    const alpha = color === 'A' ? newValue : value.alpha;

    const newColor = new Color().setRgba(red, green, blue, alpha);
    const hue = new Color().setHsva(newColor.getHsva().hue);

    this.hueChange.emit(hue);
    this.colorChange.emit(newColor);
  }
}
