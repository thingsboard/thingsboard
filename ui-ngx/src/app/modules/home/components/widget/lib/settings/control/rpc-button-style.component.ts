// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface RpcButtonStyle {
  isRaised: boolean;
  isPrimary: boolean;
  bgColor: string;
  textColor: string;
}

@Component({
    selector: 'tb-rpc-button-style',
    templateUrl: './rpc-button-style.component.html',
    styleUrls: ['./../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RpcButtonStyleComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RpcButtonStyleComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  private modelValue: RpcButtonStyle;

  private propagateChange = null;

  public rpcButtonStyleFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.rpcButtonStyleFormGroup = this.fb.group({
      isRaised: [true, []],
      isPrimary: [false, []],
      bgColor: [null, []],
      textColor: [null, []]
    });
    this.rpcButtonStyleFormGroup.valueChanges.pipe(
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
      this.rpcButtonStyleFormGroup.disable({emitEvent: false});
    } else {
      this.rpcButtonStyleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: RpcButtonStyle): void {
    this.modelValue = value;
    this.rpcButtonStyleFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.rpcButtonStyleFormGroup.value;
    this.propagateChange(this.modelValue);
  }

}
