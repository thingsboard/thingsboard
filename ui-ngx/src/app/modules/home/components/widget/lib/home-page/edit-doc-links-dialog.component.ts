///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DocumentationLink, DocumentationLinks } from '@shared/models/user-settings.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { UserSettingsService } from '@core/http/user-settings.service';

export interface EditDocLinksDialogData {
  docLinks: DocumentationLinks;
}

@Component({
  selector: 'tb-edit-doc-links-dialog',
  templateUrl: './edit-doc-links-dialog.component.html',
  styleUrls: ['./edit-doc-links-dialog.component.scss']
})
export class EditDocLinksDialogComponent extends
  DialogComponent<EditDocLinksDialogComponent, boolean> implements OnInit {

  updated = false;
  addMode = false;
  editMode = false;

  docLinks = this.data.docLinks;
  addingDocLink: Partial<DocumentationLink>;

  editDocLinksFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EditDocLinksDialogData,
              public dialogRef: MatDialogRef<EditDocLinksDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private userSettingsService: UserSettingsService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    const docLinksControls: Array<AbstractControl> = [];
    for (const docLink of this.docLinks.links) {
      docLinksControls.push(this.fb.control(docLink, [Validators.required]));
    }
    this.editDocLinksFormGroup = this.fb.group({
      links: this.fb.array(docLinksControls)
    });
  }

  docLinksFormArray(): UntypedFormArray {
    return this.editDocLinksFormGroup.get('links') as UntypedFormArray;
  }

  trackByDocLink(index: number, docLinkControl: AbstractControl): any {
    return docLinkControl;
  }

  docLinkDrop(event: CdkDragDrop<string[]>) {
    const docLinksArray = this.editDocLinksFormGroup.get('links') as UntypedFormArray;
    const docLink = docLinksArray.at(event.previousIndex);
    docLinksArray.removeAt(event.previousIndex);
    docLinksArray.insert(event.currentIndex, docLink);
    this.update();
  }

  addLink() {
    this.addingDocLink = { icon: 'notifications' };
    this.addMode = true;
  }

  linkAdded(docLink: DocumentationLink) {
    this.addMode = false;
    const docLinksArray = this.editDocLinksFormGroup.get('links') as UntypedFormArray;
    const docLinkControl = this.fb.control(docLink, [Validators.required]);
    docLinksArray.push(docLinkControl);
    this.update();
  }

  deleteLink(index: number) {
    (this.editDocLinksFormGroup.get('links') as UntypedFormArray).removeAt(index);
    this.update();
  }

  update() {
    if (this.editDocLinksFormGroup.valid) {
      const docLinks: DocumentationLinks = this.editDocLinksFormGroup.value;
      this.userSettingsService.updateDocumentationLinks(docLinks).subscribe(() => {
        this.updated = true;
      });
    }
  }

  close(): void {
    this.dialogRef.close(this.updated);
  }
}
