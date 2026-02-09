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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { merge } from 'rxjs';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  TripTimelineSettings
} from '@shared/models/widget/maps/map.models';

@Component({
    selector: 'tb-trip-timeline-settings',
    templateUrl: './trip-timeline-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TripTimelineSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TripTimelineSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TripTimelineSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  settingsExpanded = false;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  @Input()
  disabled: boolean;

  private modelValue: TripTimelineSettings;

  private propagateChange = null;

  public tripTimelineSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.tripTimelineSettingsFormGroup = this.fb.group({
      showTimelineControl: [null],
      timeStep: [null, [Validators.required, Validators.min(1)]],
      speedOptions: [null, [Validators.required]],
      showTimestamp: [null],
      timestampFormat: [null, [Validators.required]],
      snapToRealLocation: [null],
      locationSnapFilter: [null, [Validators.required]]
    });

    this.tripTimelineSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.tripTimelineSettingsFormGroup.get('showTimelineControl').valueChanges,
          this.tripTimelineSettingsFormGroup.get('showTimestamp').valueChanges,
          this.tripTimelineSettingsFormGroup.get('snapToRealLocation').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tripTimelineSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripTimelineSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TripTimelineSettings): void {
    this.modelValue = value;
    this.tripTimelineSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.settingsExpanded = this.tripTimelineSettingsFormGroup.get('showTimelineControl').value;
    this.tripTimelineSettingsFormGroup.get('showTimelineControl').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  public validate(c: UntypedFormControl) {
    const valid = this.tripTimelineSettingsFormGroup.valid;
    return valid ? null : {
      tripTimelineSettings: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const showTimelineControl: boolean = this.tripTimelineSettingsFormGroup.get('showTimelineControl').value;
    const showTimestamp: boolean = this.tripTimelineSettingsFormGroup.get('showTimestamp').value;
    const snapToRealLocation: boolean = this.tripTimelineSettingsFormGroup.get('snapToRealLocation').value;
    if (showTimelineControl) {
      this.tripTimelineSettingsFormGroup.enable({emitEvent: false});
      if (!showTimestamp) {
        this.tripTimelineSettingsFormGroup.get('timestampFormat').disable({emitEvent: false});
      }
      if (!snapToRealLocation) {
        this.tripTimelineSettingsFormGroup.get('locationSnapFilter').disable({emitEvent: false});
      }
    } else {
      this.tripTimelineSettingsFormGroup.disable({emitEvent: false});
      this.tripTimelineSettingsFormGroup.get('showTimelineControl').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.tripTimelineSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
