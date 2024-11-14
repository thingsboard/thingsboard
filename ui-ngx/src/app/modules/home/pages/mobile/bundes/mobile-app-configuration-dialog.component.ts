///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';

export interface MobileAppConfigurationDialogData {
  afterAdd: boolean;
  appSecret: string;
}

@Component({
  selector: 'tb-mobile-app-configuration-dialog',
  templateUrl: './mobile-app-configuration-dialog.component.html',
  styleUrls: ['./mobile-app-configuration-dialog.component.scss']
})
export class MobileAppConfigurationDialogComponent extends DialogComponent<MobileAppConfigurationDialogComponent> {

  notShowAgain = false;

  showDontShowAgain: boolean;

  gitRepositoryLink = 'git clone -b master https://github.com/thingsboard/flutter_thingsboard_app.git';
  pathToConstants = 'lib/constants/app_constants.dart';
  flutterRunCommand = 'flutter run';

  configureApi: string[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppConfigurationDialogData,
              protected dialogRef: MatDialogRef<MobileAppConfigurationDialogComponent>,
              ) {
    super(store, router, dialogRef);

    this.showDontShowAgain = this.data.afterAdd;

    this.configureApi.push(`static const thingsBoardApiEndpoint = '${window.location.origin}';`);
    this.configureApi.push(`static const thingsboardOAuth2AppSecret = '${this.data.appSecret}';`);
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayConfigurationAfterAddMobileBundle: true }));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  createMarkDownCommand(commands: string | string[]): string {
    if (Array.isArray(commands)) {
      const formatCommands: Array<string> = [];
      commands.forEach(command => formatCommands.push(this.createMarkDownSingleCommand(command)));
      return formatCommands.join(`\n<br />\n\n`);
    } else {
      return this.createMarkDownSingleCommand(commands);
    }
  }

  private createMarkDownSingleCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }
}
