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
  styleUrls: ['./add-doc-link-dialog.component.scss']
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
