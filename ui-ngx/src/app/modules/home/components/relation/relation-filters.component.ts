// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR
} from '@angular/forms';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { RelationEntityTypeFilter } from '@shared/models/relation.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-relation-filters',
    templateUrl: './relation-filters.component.html',
    styleUrls: ['./relation-filters.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RelationFiltersComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RelationFiltersComponent extends PageComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  @coerceBoolean()
  enableNotOption = false;

  relationFiltersFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.relationFiltersFormGroup = this.fb.group({
      relationFilters: this.fb.array([])
    });

    this.relationFiltersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get relationFiltersFormArray(): UntypedFormArray {
      return this.relationFiltersFormGroup.get('relationFilters') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(filters: Array<RelationEntityTypeFilter>): void {
    if (filters?.length === this.relationFiltersFormArray.length) {
      this.relationFiltersFormArray.patchValue(filters, {emitEvent: false});
    } else {
      const relationFiltersControls: Array<AbstractControl> = [];
      if (filters && filters.length) {
        filters.forEach((filter) => {
          relationFiltersControls.push(this.createRelationFilterFormGroup(filter));
        });
      }
      this.relationFiltersFormGroup.setControl('relationFilters', this.fb.array(relationFiltersControls), {emitEvent: false});
    }
  }

  public removeFilter(index: number) {
    (this.relationFiltersFormGroup.get('relationFilters') as UntypedFormArray).removeAt(index);
  }

  public addFilter() {
    const filter: RelationEntityTypeFilter = {
      relationType: null,
      entityTypes: []
    };
    this.relationFiltersFormArray.push(this.createRelationFilterFormGroup(filter));
  }

  private createRelationFilterFormGroup(filter: RelationEntityTypeFilter): AbstractControl {
    const formGroup = this.fb.group({
      relationType: [filter ? filter.relationType : null],
      entityTypes: [filter ? filter.entityTypes : []]
    });
    if (this.enableNotOption) {
      formGroup.addControl('negate', this.fb.control({value: filter?.negate ?? false, disabled: !filter?.relationType}));
      formGroup.get('relationType').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(value => {
        if (value) {
          formGroup.get('negate').enable({emitEvent: false});
        } else {
          formGroup.get('negate').disable({emitEvent: false});
        }
      });
    }
    return formGroup;
  }

  private updateModel() {
    const filters: Array<RelationEntityTypeFilter> = this.relationFiltersFormGroup.get('relationFilters').value;
    this.propagateChange(filters);
  }
}
