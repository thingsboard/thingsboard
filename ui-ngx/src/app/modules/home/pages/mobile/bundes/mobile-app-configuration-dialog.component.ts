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
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { MobileApp, MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { isNotEmptyStr } from '@core/utils';

export interface MobileAppConfigurationDialogData {
  afterAdd: boolean;
  androidApp: MobileApp;
  iosApp: MobileApp;
  bundle: MobileAppBundleInfo;
}

@Component({
    selector: 'tb-mobile-app-configuration-dialog',
    templateUrl: './mobile-app-configuration-dialog.component.html',
    styleUrls: ['./mobile-app-configuration-dialog.component.scss'],
    standalone: false
})
export class MobileAppConfigurationDialogComponent extends DialogComponent<MobileAppConfigurationDialogComponent> {

  private fileName = 'configs';

  notShowAgain = false;
  showDontShowAgain: boolean;

  gitRepositoryLink = 'git clone -b master https://github.com/thingsboard/flutter_thingsboard_app.git';
  flutterRunCommand = `flutter run --dart-define-from-file ${this.fileName}.json`;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppConfigurationDialogData,
              protected dialogRef: MatDialogRef<MobileAppConfigurationDialogComponent>,
              private importExportService: ImportExportService,
              ) {
    super(store, router, dialogRef);
    this.showDontShowAgain = this.data.afterAdd;
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayConfigurationAfterAddMobileBundle: true }));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  createMarkDownCommand(commands: string): string {
    return this.createMarkDownSingleCommand(commands);
  }

  downloadSettings(): void {
    const settings: any = {
      thingsboardApiEndpoint: window.location.origin,
      appLinksUrlHost: window.location.host,
      appLinksUrlScheme: window.location.protocol.slice(0, -1),
    };
    if (!!this.data.androidApp) {
      settings.androidApplicationId = this.data.androidApp.pkgName;
      settings.androidApplicationName = isNotEmptyStr(this.data.androidApp.title) ? this.data.androidApp.title : this.data.bundle.title;
      settings.thingsboardOAuth2CallbackUrlScheme = this.data.androidApp.pkgName + '.auth';
      settings.thingsboardAndroidAppSecret = this.data.androidApp.appSecret;
    }
    if (!!this.data.iosApp) {
      settings.iosApplicationId = this.data.iosApp.pkgName;
      settings.iosApplicationName = isNotEmptyStr(this.data.iosApp.title) ? this.data.iosApp.title : this.data.bundle.title;
      settings.thingsboardIosAppSecret = this.data.iosApp.appSecret;
    }
    this.importExportService.exportJson(settings, this.fileName);
  }

  private createMarkDownSingleCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }
}
