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

import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface Lwm2mObjectAddInstancesData {
  instancesId: Set<number>;
  objectName?: string;
  objectId?: number;
}

@Component({
  selector: 'tb-lwm2m-object-add-instances',
  templateUrl: './lwm2m-object-add-instances-dialog.component.html'
})
export class Lwm2mObjectAddInstancesDialogComponent extends DialogComponent<Lwm2mObjectAddInstancesDialogComponent, object>
  implements OnInit {

  instancesFormGroup: UntypedFormGroup;
  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: Lwm2mObjectAddInstancesData,
              public dialogRef: MatDialogRef<Lwm2mObjectAddInstancesDialogComponent, object>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.instancesFormGroup = this.fb.group({
      instancesIds: [this.data.instancesId]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.dialogRef.close(this.instancesFormGroup.get('instancesIds').value);
  }
}
