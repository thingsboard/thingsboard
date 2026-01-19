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
  styleUrls: ['api-key-generated-dialog.component.scss']
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
