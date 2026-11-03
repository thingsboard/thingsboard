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
import { DocumentationLink } from '@shared/models/user-settings.models';

@Component({
    selector: 'tb-add-doc-link-dialog',
    templateUrl: './add-doc-link-dialog.component.html',
    styleUrls: ['./add-doc-link-dialog.component.scss'],
    standalone: false
})
export class AddDocLinkDialogComponent extends
  DialogComponent<AddDocLinkDialogComponent, DocumentationLink> implements OnInit {

  addDocLinkFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddDocLinkDialogComponent, DocumentationLink>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.addDocLinkFormGroup = this.fb.group({
      docLink: [{ icon: 'notifications' }, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(docLink: DocumentationLink): void {
    this.dialogRef.close(docLink);
  }
}
