// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { EntitySearchDirection, entitySearchDirectionTranslations, PageComponent } from '@shared/public-api';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { RelationsQuery } from '../rule-node-config.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-relations-query-config-old',
    templateUrl: './relations-query-config-old.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RelationsQueryConfigOldComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RelationsQueryConfigOldComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  directionTypes = Object.keys(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;

  relationsQueryFormGroup: FormGroup;

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.relationsQueryFormGroup = this.fb.group({
      fetchLastLevelOnly: [false, []],
      direction: [null, [Validators.required]],
      maxLevel: [null, []],
      filters: [null]
    });
    this.relationsQueryFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((query: RelationsQuery) => {
      if (this.relationsQueryFormGroup.valid) {
        this.propagateChange(query);
      } else {
        this.propagateChange(null);
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.relationsQueryFormGroup.disable({emitEvent: false});
    } else {
      this.relationsQueryFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(query: RelationsQuery): void {
    this.relationsQueryFormGroup.reset(query || {}, {emitEvent: false});
  }
}
