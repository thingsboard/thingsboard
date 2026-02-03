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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { merge } from 'rxjs';
import {
  StatusWidgetLayout,
  StatusWidgetStateSettings
} from '@home/components/widget/lib/indicator/status-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-status-widget-state-settings',
    templateUrl: './status-widget-state-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => StatusWidgetStateSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class StatusWidgetStateSettingsComponent implements OnInit, OnChanges, ControlValueAccessor {

  StatusWidgetLayout = StatusWidgetLayout;

  @Input()
  disabled: boolean;

  @Input()
  layout: StatusWidgetLayout;

  private modelValue: StatusWidgetStateSettings;

  private propagateChange = null;

  public stateSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.stateSettingsFormGroup = this.fb.group({
      showLabel: [null, []],
      label: [null, []],
      labelFont: [null, []],
      showStatus: [null, []],
      status: [null, []],
      statusFont: [null, []],
      icon: [null, []],
      iconSize: [null, []],
      iconSizeUnit: [null, []],
      primaryColor: [null, []],
      secondaryColor: [null, []],
      background: [null, []],
      primaryColorDisabled: [null, []],
      secondaryColorDisabled: [null, []],
      backgroundDisabled: [null, []]
    });
    this.stateSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.stateSettingsFormGroup.get('showLabel').valueChanges,
      this.stateSettingsFormGroup.get('showStatus').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['layout'].includes(propName)) {
          this.updateValidators();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stateSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.stateSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: StatusWidgetStateSettings): void {
    this.modelValue = value;
    this.stateSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    if (this.layout === StatusWidgetLayout.icon) {
      this.stateSettingsFormGroup.get('showLabel').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('label').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('showStatus').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('status').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('statusFont').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColor').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColorDisabled').disable({emitEvent: false});
    } else {
      this.stateSettingsFormGroup.get('showLabel').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('showStatus').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColor').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColorDisabled').enable({emitEvent: false});
      const showLabel: boolean = this.stateSettingsFormGroup.get('showLabel').value;
      const showStatus: boolean = this.stateSettingsFormGroup.get('showStatus').value;
      if (showLabel) {
        this.stateSettingsFormGroup.get('label').enable({emitEvent: false});
        this.stateSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      } else {
        this.stateSettingsFormGroup.get('label').disable({emitEvent: false});
        this.stateSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      }
      if (showStatus) {
        this.stateSettingsFormGroup.get('status').enable({emitEvent: false});
        this.stateSettingsFormGroup.get('statusFont').enable({emitEvent: false});
      } else {
        this.stateSettingsFormGroup.get('status').disable({emitEvent: false});
        this.stateSettingsFormGroup.get('statusFont').disable({emitEvent: false});
      }
    }
  }

  private updateModel() {
    this.modelValue = this.stateSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
