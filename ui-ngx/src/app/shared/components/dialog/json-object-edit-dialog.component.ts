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

import {Component, Inject, OnInit} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormBuilder, FormGroup} from '@angular/forms';

export interface JsonObjectEdittDialogData {
  jsonValue: Object;
  title?: string;
}

@Component({
  selector: 'tb-object-edit-dialog',
  templateUrl: './json-object-edit-dialog.component.html',
  styleUrls: []
})
export class JsonObjectEditDialogComponent extends DialogComponent<JsonObjectEditDialogComponent, Object>
  implements OnInit {

  jsonFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: JsonObjectEdittDialogData,
              public dialogRef: MatDialogRef<JsonObjectEditDialogComponent, Object>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.jsonFormGroup = this.fb.group({
      json: [this.data.jsonValue, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  add(): void {
    this.dialogRef.close(this.jsonFormGroup.get('json').value);
  }
}
