// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';

@Component({
    selector: 'tb-add-entity-groups-to-edge-dialog',
    templateUrl: './add-entity-groups-to-edge-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AddEntityGroupsToEdgeDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class AddEntityGroupsToEdgeDialogComponent extends
  DialogComponent<AddEntityGroupsToEdgeDialogComponent> implements OnInit, ErrorStateMatcher {

  addEntityGroupToEdgeFormGroup: UntypedFormGroup;

  submitted = false;

  entityType = EntityType;

  groupType: EntityType;
  edgeId: string;
  customerId: string;
  childGroupId: string;
  addEntityGroupsToEdgeTitle: string;
  confirmSelectTitle: string;
  notFoundText: string;
  requiredText: string;

  edgeEntityGroupIds: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected userPermissionsService: UserPermissionsService,
              protected entityGroupService: EntityGroupService,
              protected broadcast: BroadcastService,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityGroupsToEdgeDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntityGroupsToEdgeDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.groupType = data.groupType;
    this.edgeId = data.edgeId;
    this.addEntityGroupsToEdgeTitle = data.addEntityGroupsToEdgeTitle;
    this.confirmSelectTitle = data.confirmSelectTitle;
    this.notFoundText = data.notFoundText;
    this.requiredText = data.requiredText;
    this.edgeEntityGroupIds = [];
  }

  ngOnInit(): void {
    this.addEntityGroupToEdgeFormGroup = this.fb.group({
      edgeEntityGroupIds: [[...this.edgeEntityGroupIds]]
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  addEntityGroupsToEdge(): void {
    this.submitted = true;
    const edgeEntityGroupIds: Array<string> = this.addEntityGroupToEdgeFormGroup.get('edgeEntityGroupIds').value;
    const tasks: Observable<any>[] = [];
    edgeEntityGroupIds.forEach(
      (entityGroupId) => {
        tasks.push(this.entityGroupService.assignEntityGroupToEdge(this.edgeId, entityGroupId, this.groupType));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

}
