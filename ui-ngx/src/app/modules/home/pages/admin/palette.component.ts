// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  Palette, tbAccentPalette,
  tbLoginAccentPalette,
  tbLoginPrimaryPalette,
  tbPrimaryPalette
} from '@shared/models/white-labeling.models';
import { deepClone, isEqual } from '@core/utils';
import { ColorPalette, getContrastColor, materialColorPalette } from '@shared/models/material.models';
import { PaletteDialogComponent, PaletteDialogData } from '@home/pages/admin/palette-dialog.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-palette',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => PaletteComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PaletteComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  @coerceBoolean()
  isLogin = false;

  @Input()
  @coerceBoolean()
  isPrimary = true;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  palette: Palette;

  palettes: Palette[] = [];

  private propagateChange = null;

  private defaultPaletteInfo: ColorPalette;
  private defaultPaletteContrastColorKey: string;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog) {
    super(store);
    this.palettes.push({
      type: 'default'
    });
    for (const paletteType of Object.keys(materialColorPalette)) {
      const palette: Palette = {
        type: paletteType
      };
      this.palettes.push(palette);
    }
  }

  ngOnInit(): void {
    if (this.isLogin) {
      this.defaultPaletteInfo = this.isPrimary ? tbLoginPrimaryPalette : tbLoginAccentPalette;
    } else {
      this.defaultPaletteInfo = this.isPrimary ? tbPrimaryPalette : tbAccentPalette;
    }
    this.defaultPaletteContrastColorKey = this.isPrimary ? 'teal' : 'deep-orange';
  }

  ngAfterViewInit() {
  }

  ngOnDestroy() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Palette): void {
    this.palette = deepClone(value);
    if (!this.palette || !this.palette.type) {
      this.palette = {
        type: 'default'
      };
    }
    if (this.palette.type === 'custom') {
      const customPaletteIndex = this.palettes.findIndex((palette) => palette.type === 'custom');
      if (customPaletteIndex === -1) {
        this.palettes.push(deepClone(this.palette));
      } else {
        this.palettes[customPaletteIndex] = deepClone(this.palette);
      }
    }
  }

  paletteName(palette: Palette): string {
    if (palette) {
      const str = palette.type.replace('-', ' ');
      return str.charAt(0).toUpperCase() + str.slice(1);
    } else {
      return '';
    }
  }

  paletteStyle(palette: Palette): {[klass: string]: any} {
    if (palette && palette.type) {
      const key = palette.type === 'custom' ? palette.extends : palette.type;
      let paletteInfo: ColorPalette;
      if (key === 'default') {
        paletteInfo = this.defaultPaletteInfo;
      } else {
        paletteInfo = materialColorPalette[key];
      }
      const hex = palette.colors && palette.colors['500']
          ? palette.colors['500'] : paletteInfo['500'];
      let contrastColorKey = key;
      if (key === 'default') {
        contrastColorKey = this.defaultPaletteContrastColorKey;
      }
      const contrast = getContrastColor(contrastColorKey, '500');
      return {
        '--tb-palette-preview-background': hex,
        '--tb-palette-preview-color': contrast
      }
    } else {
      return {};
    }
  }

  paletteTypeChanged() {
    if (this.palette.type === 'custom') {
      const customPaletteResult = this.palettes.find((palette) => palette.type === 'custom');
      if (customPaletteResult) {
        this.palette = deepClone(customPaletteResult);
      }
    } else {
      delete this.palette.extends;
      delete this.palette.colors;
    }
    this.updateModel();
  }

  editPalette() {
    this.dialog.open<PaletteDialogComponent, PaletteDialogData, ColorPalette>(PaletteDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          palette: deepClone(this.palette),
          defaultPaletteInfo: this.defaultPaletteInfo,
          defaultPaletteContrastColorKey: this.defaultPaletteContrastColorKey
        }
    }).afterClosed().subscribe((colors) => {
      if (colors) {
        if (isEqual(colors, {})) {
          colors = null;
        }
        this.updatePaletteColors(colors);
      }
    });
  }

  private updatePaletteColors(colors: ColorPalette) {
    if (colors) {
      this.palette.colors = colors;
      if (this.palette.type !== 'custom') {
        this.palette.extends = this.palette.type;
        this.palette.type = 'custom';
      }
      const customPaletteIndex = this.palettes.findIndex((palette) => palette.type === 'custom');
      if (customPaletteIndex === -1) {
        this.palettes.push(deepClone(this.palette));
      } else {
        this.palettes[customPaletteIndex] = deepClone(this.palette);
      }
    } else {
      delete this.palette.colors;
      if (this.palette.type === 'custom') {
        this.palette.type = this.palette.extends;
        delete this.palette.extends;
      }
    }
    this.updateModel();
  }

  private updateModel() {
    if (this.palette.type === 'default') {
      this.propagateChange({
        type: null
      });
    } else {
      this.propagateChange(this.palette);
    }
  }
}
