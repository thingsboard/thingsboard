// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators,
  ValidationErrors
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { SubscriptSizing } from '@angular/material/form-field';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-key-val-map',
    templateUrl: './kv-map.component.html',
    styleUrls: ['./kv-map.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => KeyValMapComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => KeyValMapComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class KeyValMapComponent extends PageComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  @Input() disabled: boolean;

  @Input() @coerceBoolean() isValueRequired = true;

  @Input() titleText: string;

  @Input() keyPlaceholderText: string;

  @Input() valuePlaceholderText: string;

  @Input() noDataText: string;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  kvListFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.kvListFormGroup = this.fb.group({
      keyVals: this.fb.array([])
    });

    this.kvListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  keyValsFormArray(): UntypedFormArray {
    return this.kvListFormGroup.get('keyVals') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(keyValMap: {[key: string]: string}): void {
    const keyValsControls: Array<AbstractControl> = [];
    if (keyValMap) {
      for (const property of Object.keys(keyValMap)) {
        if (Object.prototype.hasOwnProperty.call(keyValMap, property)) {
          keyValsControls.push(this.fb.group({
            key: [property, [Validators.required]],
            value: [keyValMap[property], this.isValueRequired ? [Validators.required] : []]
          }));
        }
      }
    }
    this.kvListFormGroup.setControl('keyVals', this.fb.array(keyValsControls), {emitEvent: false});
    if (this.disabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  public removeKeyVal(index: number) {
    (this.kvListFormGroup.get('keyVals') as UntypedFormArray).removeAt(index);
  }

  public addKeyVal() {
    const keyValsFormArray = this.kvListFormGroup.get('keyVals') as UntypedFormArray;
    keyValsFormArray.push(this.fb.group({
      key: ['', [Validators.required]],
      value: ['', this.isValueRequired ? [Validators.required] : []]
    }));
  }

  public validate(): ValidationErrors | null {
    return this.kvListFormGroup.valid ? null : { keyVals: { valid: false } };
  }

  private updateModel() {
    const kvList: {key: string; value: string}[] = this.kvListFormGroup.get('keyVals').value;
    const keyValMap: {[key: string]: string} = {};
    kvList.forEach((entry) => {
      keyValMap[entry.key] = entry.value;
    });
    this.propagateChange(keyValMap);
  }
}
