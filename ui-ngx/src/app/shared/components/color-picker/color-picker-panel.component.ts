// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { PageComponent } from '@shared/components/page.component';
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormControl } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-color-picker-panel',
    templateUrl: './color-picker-panel.component.html',
    providers: [],
    styleUrls: ['./color-picker-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ColorPickerPanelComponent extends PageComponent implements OnInit {

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  colorCancelButton = false;

  @Input()
  popover: TbPopoverComponent<ColorPickerPanelComponent>;

  @Output()
  colorSelected = new EventEmitter<string>();

  @Output()
  colorCancelDialog = new EventEmitter();

  colorPickerControl: UntypedFormControl;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.colorPickerControl = new UntypedFormControl(this.color);
  }

  selectColor() {
    this.colorSelected.emit(this.colorPickerControl.value);
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
