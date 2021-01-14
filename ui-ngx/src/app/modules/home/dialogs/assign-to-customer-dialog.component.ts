///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface AssignToCustomerDialogData {
  entityIds: Array<EntityId>;
  entityType: EntityType;
}

@Component({
  selector: 'tb-assign-to-customer-dialog',
  templateUrl: './assign-to-customer-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AssignToCustomerDialogComponent}],
  styleUrls: []
})
export class AssignToCustomerDialogComponent extends
  DialogComponent<AssignToCustomerDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  assignToCustomerFormGroup: FormGroup;

  submitted = false;

  entityType = EntityType;

  assignToCustomerTitle: string;
  assignToCustomerText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AssignToCustomerDialogData,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AssignToCustomerDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.assignToCustomerFormGroup = this.fb.group({
      customerId: [null, [Validators.required]]
    });
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        this.assignToCustomerTitle = 'device.assign-device-to-customer';
        this.assignToCustomerText = 'device.assign-to-customer-text';
        break;
      case EntityType.ASSET:
        this.assignToCustomerTitle = 'asset.assign-asset-to-customer';
        this.assignToCustomerText = 'asset.assign-to-customer-text';
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToCustomerTitle = 'entity-view.assign-entity-view-to-customer';
        this.assignToCustomerText = 'entity-view.assign-to-customer-text';
        break;
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  assign(): void {
    this.submitted = true;
    const customerId: string = this.assignToCustomerFormGroup.get('customerId').value;
    const tasks: Observable<any>[] = [];
    this.data.entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToCustomerTask(customerId, entityId.id));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  private getAssignToCustomerTask(customerId: string, entityId: string): Observable<any> {
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        return this.deviceService.assignDeviceToCustomer(customerId, entityId);
      case EntityType.ASSET:
        return this.assetService.assignAssetToCustomer(customerId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToCustomer(customerId, entityId);
    }
  }

}
