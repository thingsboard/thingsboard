// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';

export interface PeConnectivityMethodPromptData {
  connectorName: string;
}

@Component({
  selector: 'tb-pe-connectivity-method-prompt',
  standalone: false,
  templateUrl: './pe-connectivity-method-prompt.component.html',
  styleUrls: ['./pe-connectivity-method-prompt.component.scss']
})
export class TbPeConnectivityMethodPromptComponent extends DialogComponent<TbPeConnectivityMethodPromptComponent> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbPeConnectivityMethodPromptComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PeConnectivityMethodPromptData
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close();
  }
}
