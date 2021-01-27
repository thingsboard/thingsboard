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

import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface Lwm2mObjectAddInstancesData {
  instancesIds: Set<number>;
  objectName?: string;
  objectId?: number;
}

@Component({
  selector: 'tb-lwm2m-object-add-instances',
  templateUrl: './lwm2m-object-add-instances.component.html',
  styleUrls: []
})
export class Lwm2mObjectAddInstancesComponent extends DialogComponent<Lwm2mObjectAddInstancesComponent, object> implements OnInit {

  instancesFormGroup: FormGroup;
  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: Lwm2mObjectAddInstancesData,
              public dialogRef: MatDialogRef<Lwm2mObjectAddInstancesComponent, object>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.instancesFormGroup = this.fb.group({
      instancesIds: this.data.instancesIds
    });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  add(): void {
    this.data.instancesIds = this.instancesFormGroup.get('instancesIds').value;
    this.dialogRef.close(this.data);
  }
}
