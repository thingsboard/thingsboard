// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';

export interface SaveWidgetTypeAsDialogResult {
  widgetName: string;
  widgetBundleId?: string;
}

export interface SaveWidgetTypeAsDialogData {
  dialogTitle?: string;
  title?: string;
  saveAsActionTitle?: string;
}

@Component({
    selector: 'tb-save-widget-type-as-dialog',
    templateUrl: './save-widget-type-as-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class SaveWidgetTypeAsDialogComponent extends
  DialogComponent<SaveWidgetTypeAsDialogComponent, SaveWidgetTypeAsDialogResult> implements OnInit {

  saveWidgetTypeAsFormGroup: FormGroup;
  bundlesScope: string;
  dialogTitle = 'widget.save-widget-as';
  saveAsActionTitle = 'action.saveAs';

  showSelectWidgetBundle = true;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: SaveWidgetTypeAsDialogData,
              public dialogRef: MatDialogRef<SaveWidgetTypeAsDialogComponent, SaveWidgetTypeAsDialogResult>,
              public fb: FormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store, router, dialogRef);

    const authUser = getCurrentAuthUser(store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      this.bundlesScope = 'tenant';
    } else {
      this.bundlesScope = 'system';
    }

    this.showSelectWidgetBundle = this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.WRITE);

    if (this.data?.dialogTitle) {
      this.dialogTitle = this.data.dialogTitle;
    }
    if (this.data?.saveAsActionTitle) {
      this.saveAsActionTitle = this.data.saveAsActionTitle;
    }
  }

  ngOnInit(): void {
    this.saveWidgetTypeAsFormGroup = this.fb.group({
      title: [this.data?.title, [Validators.required]],
      widgetsBundle: [null]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  saveAs(): void {
    const widgetName: string = this.saveWidgetTypeAsFormGroup.get('title').value;
    const widgetBundleId: string = this.saveWidgetTypeAsFormGroup.get('widgetsBundle').value?.id?.id;
    const result: SaveWidgetTypeAsDialogResult = {
      widgetName,
      widgetBundleId
    };
    this.dialogRef.close(result);
  }
}
