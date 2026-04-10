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
