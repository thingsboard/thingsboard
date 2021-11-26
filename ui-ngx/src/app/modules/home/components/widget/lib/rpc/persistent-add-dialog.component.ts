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

import { Component, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RequestData } from '@shared/models/rpc.models';

@Component({
  selector: 'tb-persistent-add-dialog',
  templateUrl: './persistent-add-dialog.component.html',
  styleUrls: ['./persistent-add-dialog.component.scss']
})

export class PersistentAddDialogComponent extends DialogComponent<PersistentAddDialogComponent, RequestData> implements OnInit {

  public persistentFormGroup: FormGroup;

  private requestData: RequestData = {
    persistentUpdated: false
  };

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<PersistentAddDialogComponent, RequestData>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.persistentFormGroup = this.fb.group(
      {
        method: ['', [Validators.required, Validators.pattern(/^\S+$/)]],
        oneWayElseTwoWay: [false],
        retries: [null, [Validators.pattern(/^-?[0-9]+$/), Validators.min(0)]],
        params: [{}],
        additionalInfo: [{}]
      }
    );
  }

  save() {
    if (this.persistentFormGroup.valid) {
      this.requestData = {
        persistentUpdated: true,
        method: this.persistentFormGroup.get('method').value,
        oneWayElseTwoWay: this.persistentFormGroup.get('oneWayElseTwoWay').value,
        params: this.persistentFormGroup.get('params').value,
        additionalInfo: this.persistentFormGroup.get('additionalInfo').value,
        retries:  this.persistentFormGroup.get('retries').value
      };
      this.close();
    }
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close(this.requestData);
  }
}
