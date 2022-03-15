///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { ActionStatus, AuditLog } from '@shared/models/audit-log.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface AuditLogDetailsDialogData {
  auditLog: AuditLog;
}

@Component({
  selector: 'tb-audit-log-details-dialog',
  templateUrl: './audit-log-details-dialog.component.html',
  styleUrls: ['./audit-log-details-dialog.component.scss']
})
export class AuditLogDetailsDialogComponent extends DialogComponent<AuditLogDetailsDialogComponent> implements OnInit {

  auditLog: AuditLog;
  displayFailureDetails: boolean;
  actionData: string;
  actionFailureDetails: string;
  editorStyle = {
    width: '100%',
    'min-width': '400px',
    height: '100%',
    'min-height': '50px'
  };

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AuditLogDetailsDialogData,
              public dialogRef: MatDialogRef<AuditLogDetailsDialogComponent>) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.auditLog = this.data.auditLog;
    this.displayFailureDetails = this.auditLog.actionStatus === ActionStatus.FAILURE;
    this.actionData = this.auditLog.actionData;
    this.actionFailureDetails = this.auditLog.actionFailureDetails;
  }
}
