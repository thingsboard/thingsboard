// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, forwardRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { accentPalette, ColorPalette, getContrastColor, primaryPalette } from '@shared/models/material.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { plainColorFromVariable } from '@app/core/utils';

@Component({
    selector: 'tb-theme-color-select',
    templateUrl: './theme-color-select.component.html',
    styleUrls: ['./theme-color-select.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ThemeColorSelectComponent),
            multi: true
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ThemeColorSelectComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  palette: 'primary' | 'accent' = 'primary';

  private modelValue: string;

  private propagateChange = null;

  paletteInfo: ColorPalette;
  hues: string[];
  colors: ColorPalette;
  paletteKey: string;
  selectedHue: string;

  constructor(protected store: Store<AppState>,
              private wl: WhiteLabelingService) {
    super(store);
  }

  ngOnInit(): void {
    this.paletteInfo = this.palette === 'primary' ? primaryPalette : accentPalette;
    const palette = this.palette === 'primary' ? this.wl.primaryPalette : this.wl.accentPalette;
    this.paletteKey = ['custom', 'tb-primary', 'tb-accent'].includes(palette.type) ? palette.extends : palette.type;
    this.hues = Object.keys(this.paletteInfo);
    this.colors = {};
    for (const hue of this.hues) {
      this.colors[hue] = plainColorFromVariable(this.paletteInfo[hue]);
    }
  }

  hueStyle(hue: string): {[klass: string]: any} {
    if (hue) {
      const color = this.colors[hue];
      const contrast = getContrastColor(this.paletteKey, hue);
      return {
        backgroundColor: color,
        color: contrast
      };
    } else {
      return {};
    }
  }

  selectedColorStyle(): {[klass: string]: any} {
    if (this.selectedHue) {
      const color = this.colors[this.selectedHue];
      return {
        background: color
      };
    } else {
      return {};
    }
  }

  updateValidators() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string): void {
    this.modelValue = value;
    let hue = null;
    if (value) {
      const colorIndex = Object.values(this.paletteInfo).indexOf(value);
      if (colorIndex > -1) {
        hue = this.hues[colorIndex];
      }
    }
    this.selectedHue = hue;
  }

  selectColor(hue: string) {
    if (hue) {
      this.selectedHue = hue;
      const color = this.paletteInfo[hue];
      if (this.modelValue !== color) {
        this.modelValue = color;
        this.propagateChange(this.modelValue);
      }
    }
  }

}
