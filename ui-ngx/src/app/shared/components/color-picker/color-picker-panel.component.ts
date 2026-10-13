// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { PageComponent } from '@shared/components/page.component';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { accentPalette, primaryPalette } from '@shared/models/material.models';
import { isDefinedAndNotNull, plainColorFromVariable } from '@core/utils';
import { UntypedFormControl } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

type ColorMode = 'color' | 'primary' | 'accent';

@Component({
    selector: 'tb-color-picker-panel',
    templateUrl: './color-picker-panel.component.html',
    providers: [],
    styleUrls: ['./color-picker-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ColorPickerPanelComponent extends PageComponent implements OnInit, OnDestroy{

  @Input()
  color: string;

  @Input()
  defaultColor = '#fff';

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  useThemePalette: boolean;

  @Input()
  @coerceBoolean()
  disableAlpha = false;

  @Input()
  @coerceBoolean()
  colorCancelButton = false;

  @Input()
  popover: TbPopoverComponent<ColorPickerPanelComponent>;

  @Output()
  colorSelected = new EventEmitter<string>();

  @Output()
  colorCancelDialog = new EventEmitter();

  colorMode: ColorMode = 'color';
  plainColorControl = new UntypedFormControl();
  primaryColor: string;
  accentColor: string;

  dirty = false;
  valid = true;

  private destroy$ = new Subject<void>();


  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.plainColorControl.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.onPlainColorChange();
    });
    if (this.useThemePalette) {
      if (this.color && this.color.startsWith('var(')) {
        this.colorMode = 'primary';
        if (Object.values(primaryPalette).indexOf(this.color) > -1) {
          this.primaryColor = this.color;
        } else if (Object.values(accentPalette).indexOf(this.color) > -1) {
          this.colorMode = 'accent';
          this.accentColor = this.color;
        }
        this.plainColorControl.patchValue(plainColorFromVariable(this.color), {emitEvent: false});
      } else {
        this.plainColorControl.patchValue(this.color, {emitEvent: false});
      }
    } else {
      this.plainColorControl.patchValue(this.color, {emitEvent: false});
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  onPrimaryColorChange(color: string) {
    this.primaryColor = color;
    this.accentColor = null;
    this.plainColorControl.patchValue(plainColorFromVariable(this.primaryColor), {emitEvent: false});
    this.dirty = true;
    this.updateValidity();
  }

  onAccentColorChange(color: string) {
    this.accentColor = color;
    this.primaryColor = null;
    this.plainColorControl.patchValue(plainColorFromVariable(this.accentColor), {emitEvent: false});
    this.dirty = true;
    this.updateValidity();
  }

  onPlainColorChange() {
    this.primaryColor = null;
    this.accentColor = null;
    this.dirty = true;
    this.updateValidity();
  }

  selectedIndexChange(index: number) {
    switch (index) {
      case 0:
        this.colorMode = 'color';
        break;
      case 1:
        this.colorMode = 'primary';
        break;
      case 2:
        this.colorMode = 'accent';
        break;
    }
    this.dirty = true;
    this.updateValidity();
  }

  private updateValidity() {
    const color = this.getColor();
    this.valid = isDefinedAndNotNull(color);
  }

  selectColor() {
    const color = this.getColor();
    this.colorSelected.emit(color);
  }

  getColor() {
    switch (this.colorMode) {
      case 'color':
        return this.plainColorControl.value;
      case 'primary':
        return this.primaryColor;
      case 'accent':
        return this.accentColor;
    }
  }

  clearColor() {
    this.colorSelected.emit(null);
  }

  cancelColor() {
    if (this.popover) {
      this.popover.hide();
    } else {
      this.colorCancelDialog.emit();
    }
  }}
