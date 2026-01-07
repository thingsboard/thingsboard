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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MapEditorSettings } from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-map-editor-settings',
  templateUrl: './map-editor-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapEditorSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapEditorSettingsComponent),
      multi: true
    }
  ]
})
export class MapEditorSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: MapEditorSettings;

  private propagateChange = null;

  public mapEditorSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.mapEditorSettingsFormGroup = this.fb.group({
      snappable: [null, []],
      initDragMode: [null, []],
      hideAllControlButton: [null, []],
      hideDrawControlButton: [null, []],
      hideEditControlButton: [null, []],
      hideRemoveControlButton: [null, []],
    });
    this.mapEditorSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.mapEditorSettingsFormGroup.get('hideAllControlButton').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mapEditorSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.mapEditorSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: MapEditorSettings): void {
    this.modelValue = value;
    this.mapEditorSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.mapEditorSettingsFormGroup.valid ? null : {
      mapEditorSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: MapEditorSettings = this.mapEditorSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const hideAllControlButton: boolean = this.mapEditorSettingsFormGroup.get('hideAllControlButton').value;
    if (hideAllControlButton) {
      this.mapEditorSettingsFormGroup.get('hideDrawControlButton').disable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideEditControlButton').disable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').disable({emitEvent});
    } else {
      this.mapEditorSettingsFormGroup.get('hideDrawControlButton').enable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideEditControlButton').enable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').enable({emitEvent});
    }
    this.mapEditorSettingsFormGroup.get('hideDrawControlButton').updateValueAndValidity({emitEvent: false});
    this.mapEditorSettingsFormGroup.get('hideEditControlButton').updateValueAndValidity({emitEvent: false});
    this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').updateValueAndValidity({emitEvent: false});
  }
}
