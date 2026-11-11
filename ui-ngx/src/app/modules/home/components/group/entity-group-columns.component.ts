// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  entityGroupEntityFields,
  EntityGroupSortOrder
} from '@shared/models/entity-group.models';
import { Subject } from 'rxjs';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-entity-group-columns',
    templateUrl: './entity-group-columns.component.html',
    styleUrls: ['./entity-group-columns.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityGroupColumnsComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityGroupColumnsComponent extends PageComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() entityType: EntityType;

  columnsFormGroup: UntypedFormGroup;

  private propagateChange = null;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.columnsFormGroup = this.fb.group(
      {
        columns: this.fb.array([])
      }
    );
    this.columnsFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get columnsFormArray(): UntypedFormArray {
    return this.columnsFormGroup.get('columns') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.columnsFormGroup.disable({emitEvent: false});
    } else {
      this.columnsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(columns: EntityGroupColumn[]): void {
    if (columns?.length === this.columnsFormArray.length) {
      this.columnsFormArray.patchValue(columns, {emitEvent: false});
    } else {
      const columnsControls: Array<AbstractControl> = [];
      if (columns) {
        columns.forEach((column) => {
          columnsControls.push(this.fb.control(column, [Validators.required]));
        });
      }
      this.columnsFormGroup.setControl('columns', this.fb.array(columnsControls), {emitEvent: false});
      if (this.disabled) {
        this.columnsFormGroup.disable({emitEvent: false});
      } else {
        this.columnsFormGroup.enable({emitEvent: false});
      }
    }
  }

  public removeColumn(index: number) {
    (this.columnsFormGroup.get('columns') as UntypedFormArray).removeAt(index);
  }

  public addColumn() {
    const columnsArray = this.columnsFormGroup.get('columns') as UntypedFormArray;
    columnsArray.push(this.fb.control({
      type: EntityGroupColumnType.ENTITY_FIELD,
      key: entityGroupEntityFields.name.value,
      sortOrder: EntityGroupSortOrder.NONE,
      mobileHide: false
    }, [Validators.required]));
  }

  public defaultSortOrderChanged(index: number, sortOrder?: EntityGroupSortOrder) {
    const columnsControls: UntypedFormArray = this.columnsFormGroup.get('columns') as UntypedFormArray;
    const column: EntityGroupColumn = columnsControls.at(index).value;
    sortOrder = sortOrder || column.sortOrder;
    if (sortOrder !== EntityGroupSortOrder.NONE) {
      for (let i = 0; i < columnsControls.length; i++) {
        if (i !== index) {
          const otherColumn: EntityGroupColumn = columnsControls.at(i).value;
          otherColumn.sortOrder = EntityGroupSortOrder.NONE;
          columnsControls.at(i).setValue(otherColumn);
        }
      }
    }
  }

  public updateColumn(index: number, column: EntityGroupColumn) {
    this.defaultSortOrderChanged(index, column.sortOrder);
  }

  public onDrop(event: CdkDragDrop<string[]>) {
    const columnsFormArray = this.columnsFormArray;
    const columnForm = columnsFormArray.at(event.previousIndex);
    columnsFormArray.removeAt(event.previousIndex);
    columnsFormArray.insert(event.currentIndex, columnForm);
  }

  private updateModel() {
    if (this.columnsFormGroup.valid) {
      const columns: EntityGroupColumn[] = this.columnsFormGroup.get('columns').value;
      this.propagateChange(columns);
    } else {
      this.propagateChange(null);
    }
  }
}
