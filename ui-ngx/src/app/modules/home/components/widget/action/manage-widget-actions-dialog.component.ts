// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { WidgetActionType, widgetType } from '@shared/models/widget.models';
import {
  WidgetActionCallbacks,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

export interface ManageWidgetActionsDialogData {
  widgetTitle: string;
  actionsData: WidgetActionsData;
  callbacks: WidgetActionCallbacks;
  widgetType: widgetType;
  defaultIconColor?: string;
  additionalWidgetActionTypes?: WidgetActionType[];
}

@Component({
    selector: 'tb-manage-widget-actions-dialog',
    templateUrl: './manage-widget-actions-dialog.component.html',
    providers: [],
    styleUrls: [],
    standalone: false
})
export class ManageWidgetActionsDialogComponent extends DialogComponent<ManageWidgetActionsDialogComponent,
  WidgetActionsData> implements OnInit {

  actionSources = this.data.actionsData.actionSources;
  actionsSettings: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageWidgetActionsDialogData,
              private fb: UntypedFormBuilder,
              public dialogRef: MatDialogRef<ManageWidgetActionsDialogComponent, WidgetActionsData>) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.actionsSettings = this.fb.group({
      actions: [this.data.actionsData.actionsMap, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.actionsSettings.get('actions').value);
  }

}
