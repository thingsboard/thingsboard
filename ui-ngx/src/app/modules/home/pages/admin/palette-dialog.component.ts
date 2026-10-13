// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { NgForm } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { Palette } from '@shared/models/white-labeling.models';
import { ColorPalette, getContrastColor, materialColorPalette } from '@shared/models/material.models';
import { TranslateService } from '@ngx-translate/core';
import tinycolor from 'tinycolor2';
import { DialogService } from '@core/services/dialog.service';

export interface PaletteDialogData {
  palette: Palette;
  defaultPaletteInfo: ColorPalette;
  defaultPaletteContrastColorKey: string;
}

@Component({
    selector: 'tb-palette-dialog',
    templateUrl: './palette-dialog.component.html',
    standalone: false
})
export class PaletteDialogComponent extends
  DialogComponent<PaletteDialogComponent, ColorPalette> implements OnInit {

  @ViewChild('paletteForm')
  paletteForm: NgForm;

  palette: Palette;
  colors: ColorPalette;
  paletteInfo: ColorPalette;
  hues: string[];
  paletteKey: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              private dialogs: DialogService,
              @Inject(MAT_DIALOG_DATA) public data: PaletteDialogData,
              public dialogRef: MatDialogRef<PaletteDialogComponent, ColorPalette>) {
    super(store, router, dialogRef);

    this.palette = this.data.palette;
    this.colors = this.palette.colors || {};

    this.paletteKey = this.palette.type === 'custom' ? this.palette.extends : this.palette.type;
    if (this.paletteKey === 'default') {
      this.paletteInfo = this.data.defaultPaletteInfo;
    } else {
      this.paletteInfo = materialColorPalette[this.paletteKey];
    }
    this.hues = Object.keys(this.paletteInfo);
    for (const hue of this.hues) {
      if (!this.colors[hue]) {
        this.colors[hue] = this.paletteInfo[hue];
      }
    }
  }

  ngOnInit(): void {
  }

  hueStyle(hue: string): {[klass: string]: any} {
    if (hue) {
      const hex = this.colors[hue];
      const contrastColorKey = this.paletteKey === 'default' ? this.data.defaultPaletteContrastColorKey : this.paletteKey;
      const contrast = getContrastColor(contrastColorKey, hue);
      return {
        backgroundColor: hex,
        color: contrast
      };
    } else {
      return {};
    }
  }

  hueName(hue: string): string {
    if (hue === '500') {
      return this.translate.instant('white-labeling.primary-background');
    }
    if (hue === '600') {
      return this.translate.instant('white-labeling.secondary-background');
    }
    if (hue === '300') {
      return this.translate.instant('white-labeling.hue1');
    }
    if (hue === '800') {
      return this.translate.instant('white-labeling.hue2');
    }
    if (hue === 'A100') {
      return this.translate.instant('white-labeling.hue3');
    }
    return '';
  };

  editColor(hue: string) {
    this.dialogs.colorPicker(tinycolor(this.colors[hue]).toRgbString()).subscribe((result) => {
      if (!result?.canceled) {
        this.colors[hue] = tinycolor(result?.color).toHexString();
        this.paletteForm.form.markAsDirty();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  savePalette(): void {
    this.paletteForm.form.markAsPristine();
    this.dialogRef.close(this.normalizeColors(this.colors));
  }

  private normalizeColors(colors: ColorPalette): ColorPalette {
    for (const hue of Object.keys(colors)) {
      const origHex = this.paletteInfo[hue];
      if (colors[hue] === origHex) {
        delete colors[hue];
      }
    }
    return colors;
  }

}
