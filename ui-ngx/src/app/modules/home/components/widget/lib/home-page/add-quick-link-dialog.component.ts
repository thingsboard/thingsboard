// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-add-quick-link-dialog',
    templateUrl: './add-quick-link-dialog.component.html',
    styleUrls: ['./add-quick-link-dialog.component.scss'],
    standalone: false
})
export class AddQuickLinkDialogComponent extends
  DialogComponent<AddQuickLinkDialogComponent, string> implements OnInit {

  addQuickLinkFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddQuickLinkDialogComponent, string>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.addQuickLinkFormGroup = this.fb.group({
      link: [null, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(link: string): void {
    this.dialogRef.close(link);
  }
}
