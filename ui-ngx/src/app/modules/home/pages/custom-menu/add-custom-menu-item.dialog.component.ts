// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CMItemLinkType, CMItemType, CMScope, CustomMenuItem } from '@shared/models/custom-menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

export interface AddCustomMenuItemDialogData {
  scope: CMScope;
  subItem: boolean;
}

@Component({
    selector: 'tb-add-custom-menu-item-dialog',
    templateUrl: './add-custom-menu-item-dialog.component.html',
    styleUrls: ['./add-custom-menu-item-dialog.component.scss'],
    standalone: false
})
export class AddCustomMenuItemDialogComponent
  extends DialogComponent<AddCustomMenuItemDialogComponent, CustomMenuItem> implements OnInit, OnDestroy {

  title: string;

  customMenuItemControl: UntypedFormControl;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddCustomMenuItemDialogData,
              public dialogRef: MatDialogRef<AddCustomMenuItemDialogComponent, CustomMenuItem>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    const menuItem: CustomMenuItem = {
      visible: true,
      icon: 'star',
      name: '',
      menuItemType: CMItemType.LINK,
      linkType: CMItemLinkType.URL,
      url: '',
      setAccessToken: true,
      dashboardId: null,
      hideDashboardToolbar: true
    };
    this.customMenuItemControl = this.fb.control(menuItem);
    this.title = this.data.subItem ? 'custom-menu.add-custom-menu-subitem' : 'custom-menu.add-custom-menu-item';
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit() {
    if (this.customMenuItemControl.valid) {
      const menuItem: CustomMenuItem = this.customMenuItemControl.value;
      this.dialogRef.close(menuItem);
    }
  }
}
