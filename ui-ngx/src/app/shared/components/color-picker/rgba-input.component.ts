// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';
import { coerceBoolean } from '@shared/decorators/coercion';

type Channel = 'R' | 'G' | 'B';

@Component({
  selector: 'tb-rgba-input',
  templateUrl: './rgba-input.component.html',
  styleUrl: './color-input.base.scss',
  standalone: false
})
export class RgbaInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  @coerceBoolean()
  public labelVisible = false;

  @Input()
  public suffixValue = '%';

  public get value(): ReturnType<Color['getRgba']> {
    return this.color.getRgba();
  }

  public get alphaValue(): string {
    return this.color ? Math.round(this.color.getRgba().getAlpha() * 100).toString() : '';
  }

  public onAlphaInputChange(inputValue: number): void {
    if (!this.color) return;
    const color = this.color.getRgba();
    const alpha = +inputValue / 100;
    if (color.getAlpha() !== alpha) {
      const newColor = new Color().setRgba(color.getRed(), color.getGreen(), color.getBlue(), alpha).toRgbaString();
      this.colorChange.emit(new Color(newColor));
    }
  }

  onInputChange(newValue: number, channel: Channel) {
    if (!this.color) return;
    const rgba = this.value;
    const red   = channel === 'R' ? newValue : rgba.getRed();
    const green = channel === 'G' ? newValue : rgba.getGreen();
    const blue  = channel === 'B' ? newValue : rgba.getBlue();
    if (red === rgba.getRed() && green === rgba.getGreen() && blue === rgba.getBlue()) return;
    this.colorChange.emit(new Color().setRgba(red, green, blue, rgba.alpha));
  }
}
