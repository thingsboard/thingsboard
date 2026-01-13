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

import { Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  defaultMapActionButtonSettings,
  MapActionButtonSettings
} from '@shared/models/widget/maps/map.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-map-action-button-settings',
  templateUrl: './map-action-buttons-settings.component.html',
  providers: [{
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapActionButtonsSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapActionButtonsSettingsComponent),
      multi: true
    }]
})
export class MapActionButtonsSettingsComponent implements ControlValueAccessor, Validator {

  mapActionButtonsForm = this.fb.array<MapActionButtonSettings>([]);

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.mapActionButtonsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.propagateChange(value));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.mapActionButtonsForm.disable({emitEvent: false});
    } else {
      this.mapActionButtonsForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mapActionButtonsForm.valid ? null : {
      mapActionButtons: false
    };
  }

  writeValue(buttons: MapActionButtonSettings[] = []) {
    if (buttons?.length === this.mapActionButtonsForm.length) {
      this.mapActionButtonsForm.patchValue(buttons, {emitEvent: false});
    } else {
      this.mapActionButtonsForm.clear({emitEvent: false});
      buttons.forEach(
        button => this.mapActionButtonsForm.push(this.fb.control(button), {emitEvent: false})
      );
    }
  }

  get dragEnabled(): boolean {
    return this.mapActionButtonsForm.length > 1;
  }

  buttonDrop(event: CdkDragDrop<string[]>) {
    const actionButton = this.mapActionButtonsForm.at(event.previousIndex);
    this.mapActionButtonsForm.removeAt(event.previousIndex, {emitEvent: false});
    this.mapActionButtonsForm.insert(event.currentIndex, actionButton);
  }

  addButton() {
    this.mapActionButtonsForm.push(this.fb.control(defaultMapActionButtonSettings));
  }

  removeButton(index: number) {
    this.mapActionButtonsForm.removeAt(index);
  }
}
