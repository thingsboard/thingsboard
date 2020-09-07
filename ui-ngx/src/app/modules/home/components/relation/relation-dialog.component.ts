///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import {
  CONTAINS_TYPE,
  EntityRelation,
  EntitySearchDirection,
  RelationTypeGroup
} from '@shared/models/relation.models';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityId } from '@shared/models/id/entity-id';
import { forkJoin, Observable } from 'rxjs';
import { JsonObjectEditComponent } from '@shared/components/json-object-edit.component';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';

export interface RelationDialogData {
  isAdd: boolean;
  direction: EntitySearchDirection;
  relation: EntityRelation;
}

@Component({
  selector: 'tb-relation-dialog',
  templateUrl: './relation-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: RelationDialogComponent}],
  styleUrls: ['./relation-dialog.component.scss']
})
export class RelationDialogComponent extends DialogComponent<RelationDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  relationFormGroup: FormGroup;

  isAdd: boolean;
  direction: EntitySearchDirection;
  entitySearchDirection = EntitySearchDirection;

  additionalInfo: FormControl;

  @ViewChild('additionalInfoEdit', {static: true}) additionalInfoEdit: JsonObjectEditComponent;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: RelationDialogData,
              private entityRelationService: EntityRelationService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<RelationDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.direction = data.direction;
  }

  ngOnInit(): void {
    this.relationFormGroup = this.fb.group({
      type: [this.isAdd ? CONTAINS_TYPE : this.data.relation.type, [Validators.required]],
      targetEntityIds: [this.isAdd ? null :
        [this.direction === EntitySearchDirection.FROM ? this.data.relation.to : this.data.relation.from],
        [Validators.required]]
    });
    this.additionalInfo = new FormControl(this.data.relation.additionalInfo ? {...this.data.relation.additionalInfo} : null);
    if (!this.isAdd) {
      this.relationFormGroup.get('type').disable();
      this.relationFormGroup.get('targetEntityIds').disable();
    }
    this.additionalInfo.valueChanges.subscribe(
      () => {
        this.submitted = false;
      }
    );
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.submitted = true;
    this.additionalInfoEdit.validateOnSubmit();
    if (!this.relationFormGroup.invalid && !this.additionalInfo.invalid) {
      if (this.isAdd) {
        const tasks: Observable<EntityRelation>[] = [];
        const type: string = this.relationFormGroup.get('type').value;
        const entityIds: Array<EntityId> = this.relationFormGroup.get('targetEntityIds').value;
        entityIds.forEach(entityId => {
          const relation = {
            type,
            additionalInfo: this.additionalInfo.value,
            typeGroup: RelationTypeGroup.COMMON
          } as EntityRelation;
          if (this.direction === EntitySearchDirection.FROM) {
            relation.from = this.data.relation.from;
            relation.to = entityId;
          } else {
            relation.from = entityId;
            relation.to = this.data.relation.to;
          }
          tasks.push(this.entityRelationService.saveRelation(relation));
        });
        forkJoin(tasks).subscribe(
          () => {
            this.dialogRef.close(true);
          }
        );
      } else {
        const relation: EntityRelation = {...this.data.relation};
        relation.additionalInfo = this.additionalInfo.value;
        this.entityRelationService.saveRelation(relation).subscribe(
          () => {
            this.dialogRef.close(true);
          }
        );
      }
    }
  }
}
