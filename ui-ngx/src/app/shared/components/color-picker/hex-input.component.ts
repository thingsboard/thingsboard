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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';

@Component({
    selector: `tb-hex-input`,
    templateUrl: `./hex-input.component.html`,
    styleUrls: ['./hex-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class HexInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  public labelVisible = false;

  @Input()
  public prefixValue = '#';

  public get value() {
    return this.color ? this.color.toHexString(this.color.getRgba().alpha < 1).replace('#', '') : '';
  }

  public get copyColor() {
    return this.prefixValue + this.value;
  }

  public get hueValue(): string {
    return this.color ? Math.round(this.color.getRgba().alpha * 100).toString() : '';
  }

  public onHueInputChange(event: KeyboardEvent, inputValue: string): void {
    const color = this.color.getRgba();
    const alpha = +inputValue / 100;
    if (color.getAlpha() !== alpha) {
      const newColor = new Color().setRgba(color.red, color.green, color.blue, alpha).toHexString(true);
      this.colorChange.emit(new Color(newColor));
    }
  }

  public onInputChange(event: KeyboardEvent, inputValue: string): void {
    const value = inputValue.replace('#', '').toLowerCase();
    if (
      ((event.keyCode === 13 || event.key.toLowerCase() === 'enter') && value.length === 3)
      || value.length === 6 || value.length === 8
    ) {
      const hex = parseInt(value, 16);
      const hexStr = hex.toString(16);
      if (hexStr.padStart(value.length, '0') === value && this.value.toLowerCase() !== value) {
        const newColor = new Color(`#${value}`);
        this.colorChange.emit(newColor);
      }
    }
  }
}
