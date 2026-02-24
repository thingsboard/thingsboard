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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';
import { coerceBoolean } from '@shared/decorators/coercion';

type Channel = 'H' | 'S' | 'L';

@Component({
  selector: 'tb-hsla-input',
  templateUrl: './hsla-input.component.html',
  styleUrl: './color-input.base.scss',
  standalone: false
})
export class HslaInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  @coerceBoolean()
  public labelVisible = false;

  @Input()
  public suffixValue = '%';

  public get value() {
    return this.color.getHsla();
  }

  public get alphaValue(): number {
    return this.color ? Math.round(this.color.getHsla().getAlpha() * 100) : 0;
  }

  public onAlphaInputChange(inputValue: number): void {
    if (!this.color) return;
    const hsla = this.color.getHsla();
    const alpha = +inputValue / 100;
    if (hsla.alpha !== alpha) {
      const newColor = new Color().setHsla(hsla.getHue(), hsla.getSaturation(), hsla.getLightness(), alpha);
      this.colorChange.emit(newColor);
    }
  }

  public onInputChange(newValue: number, channel: Channel): void {
    if (!this.color) return;
    const hsla = this.value;
    const hue = channel === 'H' ? +newValue : hsla.getHue();
    const saturation = channel === 'S' ? +newValue : hsla.getSaturation();
    const lightness = channel === 'L' ? +newValue : hsla.getLightness();
    if (hue === hsla.getHue() && saturation === hsla.getSaturation() && lightness === hsla.getLightness()) return;
    const newColor = new Color().setHsla(hue, saturation, lightness, hsla.getAlpha());
    this.colorChange.emit(newColor);
  }
}
