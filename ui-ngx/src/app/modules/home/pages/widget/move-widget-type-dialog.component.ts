///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

export interface MoveWidgetTypeDialogResult {
  bundleId: string;
  bundleAlias: string;
}

export interface MoveWidgetTypeDialogData {
  currentBundleId: string;
}

@Component({
  selector: 'tb-move-widget-type-dialog',
  templateUrl: './move-widget-type-dialog.component.html',
  styleUrls: []
})
export class MoveWidgetTypeDialogComponent extends
  DialogComponent<MoveWidgetTypeDialogComponent, MoveWidgetTypeDialogResult> implements OnInit {

  moveWidgetTypeFormGroup: UntypedFormGroup;

  bundlesScope: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MoveWidgetTypeDialogData,
              public dialogRef: MatDialogRef<MoveWidgetTypeDialogComponent, MoveWidgetTypeDialogResult>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    const authUser = getCurrentAuthUser(store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      this.bundlesScope = 'tenant';
    } else {
      this.bundlesScope = 'system';
    }
  }

  ngOnInit(): void {
    this.moveWidgetTypeFormGroup = this.fb.group({
      widgetsBundle: [null, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  move(): void {
    const widgetsBundle: WidgetsBundle = this.moveWidgetTypeFormGroup.get('widgetsBundle').value;
    const result: MoveWidgetTypeDialogResult = {
      bundleId: widgetsBundle.id.id,
      bundleAlias: widgetsBundle.alias
    };
    this.dialogRef.close(result);
  }
}
