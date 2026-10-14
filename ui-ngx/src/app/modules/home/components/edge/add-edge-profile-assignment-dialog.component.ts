// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { FormGroupDirective, NgForm, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { from, Observable } from 'rxjs';
import { concatMap, tap, toArray } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { EdgeAssignmentSection } from '@shared/models/edge-profile.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { IntegrationSubType } from '@shared/models/integration.models';

// ownerId/edgeId/groupType are read by tb-edge-entity-group-list through MAT_DIALOG_DATA.
export interface AddEdgeProfileAssignmentDialogData {
  edgeProfileId: string;
  section: EdgeAssignmentSection;
  ownerId: EntityId;
  edgeId: string | null;
  groupType: EntityType;
}

@Component({
  selector: 'tb-add-edge-profile-assignment-dialog',
  templateUrl: './add-edge-profile-assignment-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEdgeProfileAssignmentDialogComponent}],
  styleUrls: [],
  standalone: false
})
export class AddEdgeProfileAssignmentDialogComponent extends DialogComponent<AddEdgeProfileAssignmentDialogComponent, boolean>
  implements OnInit, ErrorStateMatcher {

  formGroup: UntypedFormGroup;
  submitted = false;
  entityType = EntityType;
  section: EdgeAssignmentSection;
  subType = '';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEdgeProfileAssignmentDialogData,
              private edgeProfileService: EdgeProfileService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEdgeProfileAssignmentDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.section = data.section;
    if (this.section.entityType === EntityType.RULE_CHAIN) {
      this.subType = RuleChainType.EDGE;
    } else if (this.section.entityType === EntityType.INTEGRATION) {
      this.subType = IntegrationSubType.EDGE;
    }
  }

  ngOnInit() {
    this.formGroup = this.fb.group({
      ids: [[], [Validators.required]]
    });
    if (this.isGroup) {
      // a profile has no owner of its own, so the groups it may carry are the tenant's and any customer's below it
      this.formGroup.addControl('ownerId', this.fb.control(this.data.ownerId, [Validators.required]));
    }
  }

  get ownerId(): EntityId {
    return this.formGroup?.get('ownerId')?.value ?? this.data.ownerId;
  }

  get isGroup(): boolean {
    return this.section.entityType === EntityType.ENTITY_GROUP;
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  add(): void {
    this.submitted = true;
    const ids: string[] = this.formGroup.get('ids').value || [];
    if (!ids.length) {
      this.dialogRef.close(false);
      return;
    }
    // Posted one by one: parallel writes race the profile row's optimistic lock and can half-apply.
    let anyApplied = false;
    from(ids).pipe(
      concatMap(id => this.request(id).pipe(tap(() => anyApplied = true))),
      toArray()
    ).subscribe({
      next: () => this.dialogRef.close(true),
      // A mid-sequence failure still leaves earlier ids applied; reload instead of claiming full success.
      error: () => this.dialogRef.close(anyApplied)
    });
  }

  private request(id: string): Observable<any> {
    const profileId = this.data.edgeProfileId;
    switch (this.section.entityType) {
      case EntityType.RULE_CHAIN: return this.edgeProfileService.addRuleChain(profileId, id);
      case EntityType.INTEGRATION: return this.edgeProfileService.addIntegration(profileId, id);
      case EntityType.SCHEDULER_EVENT: return this.edgeProfileService.addSchedulerEvent(profileId, id);
      case EntityType.ENTITY_GROUP: return this.edgeProfileService.addEntityGroup(profileId, id, this.section.groupType);
      default: throw new Error(`Unsupported edge assignment section ${this.section.key}`);
    }
  }
}
