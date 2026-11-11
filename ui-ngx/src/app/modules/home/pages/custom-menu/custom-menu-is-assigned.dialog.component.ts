// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CMAssigneeType } from '@shared/models/custom-menu.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

export interface CustomMenuIsAssignedDialogData {
  assigneeType: CMAssigneeType;
  assigneeList: EntityInfoData[];
}

@Component({
    selector: 'tb-custom-menu-is-assigned-dialog',
    templateUrl: './custom-menu-is-assigned-dialog.component.html',
    styleUrls: ['./custom-menu-is-assigned-dialog.component.scss'],
    standalone: false
})
export class CustomMenuIsAssignedDialogComponent extends
  DialogComponent<CustomMenuIsAssignedDialogComponent, boolean> implements OnInit {

  message: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CustomMenuIsAssignedDialogData,
              public dialogRef: MatDialogRef<CustomMenuIsAssignedDialogComponent, boolean>,
              public translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    if (this.data.assigneeType === CMAssigneeType.USERS) {
      this.message = this.translate.instant('custom-menu.delete-custom-menu-user-list-text');
    } else if (this.data.assigneeType === CMAssigneeType.CUSTOMERS) {
      this.message = this.translate.instant('custom-menu.delete-custom-menu-customer-list-text');
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }

  delete() {
    this.dialogRef.close(true);
  }
}
