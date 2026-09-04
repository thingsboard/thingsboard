// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder } from '@angular/forms';
import {
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { mergeMap, tap } from 'rxjs/operators';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { deepClone } from '@core/utils';

import printTemplate from './backup-code-print-template.raw';

@Component({
    selector: 'tb-backup-code-auth-dialog',
    templateUrl: './backup-code-auth-dialog.component.html',
    styleUrls: ['./authentication-dialog.component.scss'],
    standalone: false
})
export class BackupCodeAuthDialogComponent extends DialogComponent<BackupCodeAuthDialogComponent> {

  private config: AccountTwoFaSettings;
  backupCode: BackupCodeTwoFactorAuthAccountConfig;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              private importExportService: ImportExportService,
              public dialogRef: MatDialogRef<BackupCodeAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE).pipe(
      tap((data: BackupCodeTwoFactorAuthAccountConfig) => this.backupCode = data),
      mergeMap(data => this.twoFaService.verifyAndSaveTwoFaAccountConfig(data, null, {ignoreLoading: true}))
    ).subscribe((config) => {
      this.config = config;
    });
  }

  closeDialog() {
    this.dialogRef.close(this.config);
  }

  downloadFile() {
    this.importExportService.exportText(this.backupCode.codes, 'backup-codes');
  }

  printCode() {
    const codeTemplate = deepClone(this.backupCode.codes)
      .map(code => `<div class="code-row"><input type="checkbox"><span class="code">${code}</span></div>`).join('');
    const printPage = printTemplate.replace('${codesBlock}', codeTemplate);
    const newWindow = window.open('', 'Print backup code');

    newWindow.document.open();
    newWindow.document.write(printPage);

    setTimeout(() => {
      newWindow.print();

      newWindow.document.close();

      setTimeout(() => {
        newWindow.close();
      }, 10);
    }, 0);
  }
}
