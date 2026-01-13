///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { EdgeService } from '@core/http/edge.service';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DashboardService } from '@core/http/dashboard.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainType } from '@shared/models/rule-chain.models';

export interface AddEntitiesToEdgeDialogData {
  edgeId: string;
  entityType: EntityType;
}

@Component({
  selector: 'tb-add-entities-to-edge-dialog',
  templateUrl: './add-entities-to-edge-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEntitiesToEdgeDialogComponent}],
  styleUrls: []
})
export class AddEntitiesToEdgeDialogComponent extends
  DialogComponent<AddEntitiesToEdgeDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  addEntitiesToEdgeFormGroup: UntypedFormGroup;

  submitted = false;

  entityType: EntityType;
  subType: string;

  assignToEdgeTitle: string;
  assignToEdgeText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntitiesToEdgeDialogData,
              private deviceService: DeviceService,
              private edgeService: EdgeService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              private dashboardService: DashboardService,
              private ruleChainService: RuleChainService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntitiesToEdgeDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.entityType = this.data.entityType;
  }

  ngOnInit(): void {
    this.addEntitiesToEdgeFormGroup = this.fb.group({
      entityIds: [null, [Validators.required]]
    });
    this.subType = '';
    switch (this.entityType) {
      case EntityType.DEVICE:
        this.assignToEdgeTitle = 'device.assign-device-to-edge-title';
        this.assignToEdgeText = 'device.assign-device-to-edge-text';
        break;
      case EntityType.RULE_CHAIN:
        this.assignToEdgeTitle = 'rulechain.assign-rulechain-to-edge-title';
        this.assignToEdgeText = 'rulechain.assign-rulechain-to-edge-text';
        this.subType = RuleChainType.EDGE;
        break;
      case EntityType.ASSET:
        this.assignToEdgeTitle = 'asset.assign-asset-to-edge-title';
        this.assignToEdgeText = 'asset.assign-asset-to-edge-text';
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToEdgeTitle = 'entity-view.assign-entity-view-to-edge-title';
        this.assignToEdgeText = 'entity-view.assign-entity-view-to-edge-text';
        break;
      case EntityType.DASHBOARD:
        this.assignToEdgeTitle = 'dashboard.assign-dashboard-to-edge-title';
        this.assignToEdgeText = 'dashboard.assign-dashboard-to-edge-text';
        break;
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
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
        this.dialogRef.close(true);
      }
    );
  }

  private getAssignToEdgeTask(edgeId: string, entityId: string, entityType: EntityType): Observable<any> {
    switch (entityType) {
      case EntityType.DEVICE:
        return this.deviceService.assignDeviceToEdge(edgeId, entityId);
      case EntityType.ASSET:
        return this.assetService.assignAssetToEdge(edgeId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToEdge(edgeId, entityId);
      case EntityType.DASHBOARD:
        return this.dashboardService.assignDashboardToEdge(edgeId, entityId);
      case EntityType.RULE_CHAIN:
        return this.ruleChainService.assignRuleChainToEdge(edgeId, entityId);
    }
  }

}
