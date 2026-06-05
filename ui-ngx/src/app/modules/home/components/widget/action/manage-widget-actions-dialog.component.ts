///
/// Copyright © 2016-2026 The Thingsboard Authors
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
