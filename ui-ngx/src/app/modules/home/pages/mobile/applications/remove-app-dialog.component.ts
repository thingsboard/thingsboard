// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormBuilder } from '@angular/forms';
import { MobileAppService } from '@core/http/mobile-app.service';
import { mergeMap } from 'rxjs';
import { MobileAppStatus } from '@shared/models/mobile-app.models';

export interface MobileAppDeleteDialogData {
  id: string;
}

@Component({
    selector: 'tb-remove-app-dialog',
    templateUrl: './remove-app-dialog.component.html',
    styleUrls: ['./remove-app-dialog.component.scss'],
    standalone: false
})
export class RemoveAppDialogComponent extends DialogComponent<RemoveAppDialogComponent, boolean> {

  readonly deleteApplicationText: SafeHtml;
  readonly deleteVerificationText: string;

  deleteVerification = this.fb.control('');

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RemoveAppDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppDeleteDialogData,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: FormBuilder,
              private mobileAppService: MobileAppService,) {
    super(store, router, dialogRef);
    this.deleteVerificationText = this.translate.instant('mobile.delete-application-phrase');
    this.deleteApplicationText = this.sanitizer.bypassSecurityTrustHtml(
      this.translate.instant('mobile.delete-application-text', {phrase: this.deleteVerificationText})
    )
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  suspend(): void {
    this.mobileAppService.getMobileAppInfoById(this.data.id).pipe(
      mergeMap(value => {
        value.status = MobileAppStatus.SUSPENDED;
        return this.mobileAppService.saveMobileApp(value)
      })
    ).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  delete(): void {
    this.mobileAppService.deleteMobileApp(this.data.id).subscribe(() => {
      this.dialogRef.close(true);
    });
  }
}
