// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IAliasController } from '@core/api/widget-api.models';
import { AdvancedColorRange } from '@shared/models/widget-settings.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-advanced-range',
    templateUrl: './advanced-range.component.html',
    styleUrls: ['./advanced-range.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AdvancedRangeComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AdvancedRangeComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Output()
  removeAdvancedRange = new EventEmitter();

  private modelValue: AdvancedColorRange;

  private propagateChange = (v: any) => { };

  public advancedRangeLevelFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.advancedRangeLevelFormGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      color: [null, [Validators.required]]
    });
    this.advancedRangeLevelFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.advancedRangeLevelFormGroup.disable({emitEvent: false});
    } else {
      this.advancedRangeLevelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AdvancedColorRange): void {
    this.modelValue = value;
    this.advancedRangeLevelFormGroup.patchValue(value, {emitEvent: false});
  }

  private updateModel() {
    this.modelValue = this.advancedRangeLevelFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}
