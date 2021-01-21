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
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

export interface SaveWidgetTypeAsDialogResult {
  widgetName: string;
  bundleId: string;
  bundleAlias: string;
}

@Component({
  selector: 'tb-save-widget-type-as-dialog',
  templateUrl: './save-widget-type-as-dialog.component.html',
  styleUrls: []
})
export class SaveWidgetTypeAsDialogComponent extends
  DialogComponent<SaveWidgetTypeAsDialogComponent, SaveWidgetTypeAsDialogResult> implements OnInit {

  saveWidgetTypeAsFormGroup: FormGroup;

  bundlesScope: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<SaveWidgetTypeAsDialogComponent, SaveWidgetTypeAsDialogResult>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    const authUser = getCurrentAuthUser(store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      this.bundlesScope = 'tenant';
    } else {
      this.bundlesScope = 'system';
    }
  }

  ngOnInit(): void {
    this.saveWidgetTypeAsFormGroup = this.fb.group({
      title: [null, [Validators.required]],
      widgetsBundle: [null, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  saveAs(): void {
    const widgetName: string = this.saveWidgetTypeAsFormGroup.get('title').value;
    const widgetsBundle: WidgetsBundle = this.saveWidgetTypeAsFormGroup.get('widgetsBundle').value;
    const result: SaveWidgetTypeAsDialogResult = {
      widgetName,
      bundleId: widgetsBundle.id.id,
      bundleAlias: widgetsBundle.alias
    };
    this.dialogRef.close(result);
  }
}
