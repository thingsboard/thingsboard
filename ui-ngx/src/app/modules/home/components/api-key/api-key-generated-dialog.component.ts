// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { userInfoCommand, ApiKey } from '@shared/models/api-key.models';
import { getOS } from '@core/utils';

export interface ApiKeyGeneratedDialogData {
  apiKey: ApiKey;
}

@Component({
    selector: 'tb-api-key-generated-dialog',
    templateUrl: './api-key-generated-dialog.component.html',
    styleUrls: ['api-key-generated-dialog.component.scss'],
    standalone: false
})
export class ApiKeyGeneratedDialogComponent extends DialogComponent<ApiKeyGeneratedDialogComponent, void> {

  private baseUrl = window.location.origin;

  apiKeyCommand = userInfoCommand(this.baseUrl, this.data.apiKey.value);
  secureUrl = this.baseUrl.startsWith('https');
  selectedTab: number;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<ApiKeyGeneratedDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: ApiKeyGeneratedDialogData) {
    super(store, router, dialogRef);
    this.selectTabIndexForUserOS();
  }

  close(): void {
    this.dialogRef.close(null);
  }

  createMarkDownCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }

  private selectTabIndexForUserOS() {
    const currentOS = getOS();
    switch (currentOS) {
      case 'linux':
      case 'android':
        this.selectedTab = 2;
        break;
      case 'macos':
      case 'ios':
        this.selectedTab = 1;
        break;
      case 'windows':
        this.selectedTab = 0;
        break;
      default:
        this.selectedTab = 2;
    }
  }
}
