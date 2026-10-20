// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { IntegrationService } from '@core/http/integration.service';
import { IntegrationSubType } from '@shared/models/integration.models';

export interface AddEntitiesToEdgeDialogData {
  edgeId: string;
  entityType: EntityType;
}

@Component({
    selector: 'tb-add-entities-to-edge-dialog',
    templateUrl: './add-entities-to-edge-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AddEntitiesToEdgeDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class AddEntitiesToEdgeDialogComponent extends
  DialogComponent<AddEntitiesToEdgeDialogComponent, Array<string>> implements OnInit, ErrorStateMatcher {

  addEntitiesToEdgeFormGroup: UntypedFormGroup;

  submitted = false;

  entityType: EntityType;
  subType: string;
  edgeId: string;

  assignToEdgeTitle: string;
  assignToEdgeText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntitiesToEdgeDialogData,
              private ruleChainService: RuleChainService,
              private schedulerEventService: SchedulerEventService,
              private integrationService: IntegrationService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntitiesToEdgeDialogComponent, Array<string>>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.entityType = this.data.entityType;
  }

  ngOnInit(): void {
    this.addEntitiesToEdgeFormGroup = this.fb.group({
      entityIds: [null, [Validators.required]]
    });
    switch (this.entityType) {
      case EntityType.RULE_CHAIN:
        this.assignToEdgeTitle = 'rulechain.assign-rulechain-to-edge-title';
        this.assignToEdgeText = 'rulechain.assign-rulechain-to-edge-text';
        this.subType = RuleChainType.EDGE;
        break;
      case EntityType.SCHEDULER_EVENT:
        this.assignToEdgeTitle = 'edge.assign-scheduler-event-to-edge-title';
        this.assignToEdgeText = 'edge.assign-scheduler-event-to-edge-text';
        break;
      case EntityType.INTEGRATION:
        this.assignToEdgeTitle = 'edge.assign-integration-to-edge-title';
        this.assignToEdgeText = 'edge.assign-integration-to-edge-text';
        this.subType = IntegrationSubType.EDGE;
        break;
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  assign(): void {
    this.submitted = true;
    const entityIds: Array<string> = this.addEntitiesToEdgeFormGroup.get('entityIds').value;
    const tasks: Observable<any>[] = [];
    entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToEdgeTask(this.data.edgeId, entityId, this.entityType));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(entityIds);
      }
    );
  }

  private getAssignToEdgeTask(edgeId: string, entityId: string, entityType: EntityType): Observable<any> {
    switch (entityType) {
      case EntityType.RULE_CHAIN:
        return this.ruleChainService.assignRuleChainToEdge(edgeId, entityId);
      case EntityType.SCHEDULER_EVENT:
        return this.schedulerEventService.assignSchedulerEventToEdge(edgeId, entityId);
      case EntityType.INTEGRATION:
        return this.integrationService.assignIntegrationToEdge(edgeId, entityId);
    }
  }

}
