// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { Role } from '@shared/models/role.models';

export interface ViewRoleDialogData {
  role: Role;
}

@Component({
    selector: 'tb-view-role-dialog',
    templateUrl: './view-role-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class ViewRoleDialogComponent
  extends DialogComponent<ViewRoleDialogComponent> implements OnInit {

  roleFormGroup: UntypedFormGroup;

  role = this.data.role;

  roleType = RoleType;
  roleTypes = Object.values(RoleType);
  roleTypeTranslations = roleTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ViewRoleDialogData,
              public dialogRef: MatDialogRef<ViewRoleDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.roleFormGroup = this.fb.group(
      {
        name: [this.role.name, []],
        type: [this.role.type, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [this.role.additionalInfo ? this.role.additionalInfo.description : ''],
          }
        ),
        genericPermissions: [this.role.type === RoleType.GENERIC ?
          {permissions: this.role.permissions, excludedPermissions: this.role.excludedPermissions} : null, []],
        groupPermissions: [this.role.type === RoleType.GROUP ? this.role.permissions : null, []]
      }
    );
    this.roleFormGroup.disable();
  }

  close(): void {
    this.dialogRef.close();
  }

}
