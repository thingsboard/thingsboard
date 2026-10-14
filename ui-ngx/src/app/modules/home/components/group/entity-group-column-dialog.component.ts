// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  entityGroupColumnTypeTranslationMap,
  EntityGroupEntityField,
  EntityGroupSortOrder,
  entityGroupSortOrderTranslationMap
} from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { merge } from 'rxjs';

export interface EntityGroupColumnDialogData {
  isReadOnly: boolean;
  column: EntityGroupColumn;
  entityType: EntityType;
  columnTypes: EntityGroupColumnType[];
  entityFields: {[fieldName: string]: EntityGroupEntityField};
}

@Component({
    selector: 'tb-entity-group-column-dialog',
    templateUrl: './entity-group-column-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: EntityGroupColumnDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class EntityGroupColumnDialogComponent extends
  DialogComponent<EntityGroupColumnDialogComponent, EntityGroupColumn> implements OnInit, ErrorStateMatcher {

  columnFormGroup: UntypedFormGroup;

  columnType = EntityGroupColumnType;

  isReadOnly = this.data.isReadOnly;
  column = this.data.column;
  entityType = this.data.entityType;
  columnTypes = this.data.columnTypes;
  entityFields = this.data.entityFields;
  entityFieldKeys = Object.keys(this.entityFields);
  sortOrders = Object.values(EntityGroupSortOrder);

  entityGroupColumnTypeTranslations = entityGroupColumnTypeTranslationMap;
  entityGroupSortOrderTranslations = entityGroupSortOrderTranslationMap;

  submitted = false;

  functionScopeVariables: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupColumnDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityGroupColumnDialogComponent, EntityGroupColumn>,
              private widgetService: WidgetService,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
  }

  ngOnInit(): void {
    this.columnFormGroup = this.fb.group({
      type: [null, Validators.required],
      key: [null, Validators.required],
      title: [null],
      sortOrder: [null, Validators.required],
      disableSorting: [null],
      mobileHide: [null],
      useCellStyleFunction: [null],
      cellStyleFunction: [null],
      useCellContentFunction: [null],
      cellContentFunction: [null]
    });
    this.columnFormGroup.reset(this.column, {emitEvent: false});
    if (this.isReadOnly) {
      this.columnFormGroup.disable({emitEvent: false});
    } else {
      merge(
        this.columnFormGroup.get('useCellStyleFunction').valueChanges,
        this.columnFormGroup.get('useCellContentFunction').valueChanges,
        this.columnFormGroup.get('disableSorting').valueChanges
      ).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateDisabledState();
      });
      this.updateDisabledState();
    }
  }

  private updateDisabledState() {
    const useCellStyleFunction: boolean = this.columnFormGroup.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.columnFormGroup.get('useCellContentFunction').value;
    const disableSorting: boolean = this.columnFormGroup.get('disableSorting').value;
    if (useCellStyleFunction) {
      this.columnFormGroup.get('cellStyleFunction').enable({emitEvent: false});
    } else {
      this.columnFormGroup.get('cellStyleFunction').disable({emitEvent: false});
    }
    if (useCellContentFunction) {
      this.columnFormGroup.get('cellContentFunction').enable({emitEvent: false});
    } else {
      this.columnFormGroup.get('cellContentFunction').disable({emitEvent: false});
    }
    if (disableSorting) {
      this.columnFormGroup.get('sortOrder').setValue(EntityGroupSortOrder.NONE);
      this.columnFormGroup.get('sortOrder').disable({emitEvent: false});
    } else {
      this.columnFormGroup.get('sortOrder').enable({emitEvent: false});
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.columnFormGroup.valid) {
      this.column = {...this.column, ...this.columnFormGroup.getRawValue()};
      this.dialogRef.close(this.column);
    }
  }
}
