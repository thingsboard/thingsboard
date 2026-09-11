// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import {
  EntityKeyType,
  entityKeyTypeTranslationMap,
  KeyFilterInfo,
  keyFilterInfosToKeyFilters
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { KeyFilterDialogComponent, KeyFilterDialogData } from '@home/components/filter/key-filter-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-key-filter-list',
    templateUrl: './key-filter-list.component.html',
    styleUrls: ['./key-filter-list.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => KeyFilterListComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => KeyFilterListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class KeyFilterListComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() telemetryKeysOnly = false;

  @Input() entityId: EntityId;

  keyFilterListFormGroup: UntypedFormGroup;

  entityKeyTypeTranslations = entityKeyTypeTranslationMap;

  keyFiltersControl: UntypedFormControl;

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.keyFilterListFormGroup = this.fb.group({
      keyFilters: this.fb.array([])
    });
    this.keyFiltersControl = this.fb.control(null);

    this.keyFilterListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get keyFiltersFormArray(): UntypedFormArray {
    return this.keyFilterListFormGroup.get('keyFilters') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.keyFilterListFormGroup.disable({emitEvent: false});
      this.keyFiltersControl.disable({emitEvent: false});
    } else {
      this.keyFilterListFormGroup.enable({emitEvent: false});
      this.keyFiltersControl.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.keyFilterListFormGroup.valid && this.keyFiltersControl.valid ? null : {
      keyFilterList: {valid: false}
    };
  }

  writeValue(keyFilters: Array<KeyFilterInfo>): void {
    if (keyFilters.length === this.keyFiltersFormArray.length) {
      this.keyFiltersFormArray.patchValue(keyFilters, {emitEvent: false});
    } else {
      const keyFilterControls: Array<AbstractControl> = [];
      if (keyFilters) {
        for (const keyFilter of keyFilters) {
          keyFilterControls.push(this.fb.control(keyFilter, [Validators.required]));
        }
      }
      this.keyFilterListFormGroup.setControl('keyFilters', this.fb.array(keyFilterControls), {emitEvent: false});
      if (this.disabled) {
        this.keyFilterListFormGroup.disable({emitEvent: false});
      } else {
        this.keyFilterListFormGroup.enable({emitEvent: false});
      }
    }
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }

  public removeKeyFilter(index: number) {
    (this.keyFilterListFormGroup.get('keyFilters') as UntypedFormArray).removeAt(index);
  }

  public addKeyFilter() {
    const keyFiltersFormArray = this.keyFilterListFormGroup.get('keyFilters') as UntypedFormArray;
    this.openKeyFilterDialog(null).subscribe((result) => {
      if (result) {
        keyFiltersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editKeyFilter(index: number) {
    const keyFilter: KeyFilterInfo =
      (this.keyFilterListFormGroup.get('keyFilters') as UntypedFormArray).at(index).value;
    this.openKeyFilterDialog(keyFilter).subscribe(
      (result) => {
        if (result) {
          (this.keyFilterListFormGroup.get('keyFilters') as UntypedFormArray).at(index).patchValue(result);
        }
      }
    );
  }

  private openKeyFilterDialog(keyFilter?: KeyFilterInfo): Observable<KeyFilterInfo> {
    const isAdd = !keyFilter;
    if (!keyFilter) {
      keyFilter = {
        key: {
          key: '',
          type: EntityKeyType.ATTRIBUTE
        },
        valueType: null,
        value: null,
        predicates: []
      };
    }
    return this.dialog.open<KeyFilterDialogComponent, KeyFilterDialogData,
      KeyFilterInfo>(KeyFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilter: keyFilter ? (this.disabled ? keyFilter : deepClone(keyFilter)) : null,
        isAdd,
        readonly: this.disabled,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        telemetryKeysOnly: this.telemetryKeysOnly,
        entityId: this.entityId
      }
    }).afterClosed();
  }

  private updateModel() {
    const keyFilters: Array<KeyFilterInfo> = this.keyFilterListFormGroup.getRawValue().keyFilters;
    if (keyFilters.length) {
      this.propagateChange(keyFilters);
    } else {
      this.propagateChange(null);
    }
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }
}
