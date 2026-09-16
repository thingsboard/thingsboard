// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

  public get value(): ReturnType<Color['getHsla']> {
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
