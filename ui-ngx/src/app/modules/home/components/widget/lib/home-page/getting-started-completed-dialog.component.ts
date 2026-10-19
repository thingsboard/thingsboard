// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormGroup } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-getting-started-completed-dialog',
    templateUrl: './getting-started-completed-dialog.component.html',
    styleUrls: ['./getting-started-completed-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GettingStartedCompletedDialogComponent extends
  DialogComponent<GettingStartedCompletedDialogComponent, void> {

  addDocLinkFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<GettingStartedCompletedDialogComponent, void>) {
    super(store, router, dialogRef);
    dialogRef.addPanelClass('tb-getting-started-completed-dialog');
  }

  close(): void {
    this.dialogRef.close();
  }
}
