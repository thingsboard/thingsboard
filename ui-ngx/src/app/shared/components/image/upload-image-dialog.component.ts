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

import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { ImageResource, ImageResourceInfo, imageResourceType, ResourceSubType } from '@shared/models/resource.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { forkJoin } from 'rxjs';
import { blobToBase64, blobToText, updateFileContent } from '@core/utils';
import {
  emptyMetadata,
  ScadaSymbolMetadata,
  parseScadaSymbolMetadataFromContent,
  updateScadaSymbolMetadataInContent
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActionNotificationShow } from '@core/notification/notification.actions';

export interface UploadImageDialogData {
  imageSubType: ResourceSubType;
  image?: ImageResourceInfo;
}

export interface UploadImageDialogResult {
  image?: ImageResource;
  scadaSymbolContent?: string;
}

@Component({
    selector: 'tb-upload-image-dialog',
    templateUrl: './upload-image-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: UploadImageDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class UploadImageDialogComponent extends
  DialogComponent<UploadImageDialogComponent, UploadImageDialogResult> implements OnInit, ErrorStateMatcher {

  uploadImageFormGroup: UntypedFormGroup;

  uploadImage = true;

  submitted = false;

  maxResourceSize = getCurrentAuthState(this.store).maxResourceSize;

  get isScada() {
    return this.data.imageSubType === ResourceSubType.SCADA_SYMBOL;
  }

  private scadaSymbolContent: string;
  private scadaSymbolMetadata: ScadaSymbolMetadata;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              @Inject(MAT_DIALOG_DATA) public data: UploadImageDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<UploadImageDialogComponent, UploadImageDialogResult>,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.uploadImage = !this.data?.image;
    this.uploadImageFormGroup = this.fb.group({
      file: [this.data?.image?.link, [Validators.required]]
    });
    if (this.uploadImage) {
      this.uploadImageFormGroup.addControl('title', this.fb.control(null, [Validators.required]));
      if (this.isScada) {
        this.uploadImageFormGroup.get('file').valueChanges.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe((file: File) => {
          if (file) {
            blobToText(file).subscribe(content => {
              this.scadaSymbolContent = content;
              this.scadaSymbolMetadata = parseScadaSymbolMetadataFromContent(this.scadaSymbolContent);
              const titleControl = this.uploadImageFormGroup.get('title');
              if (this.scadaSymbolMetadata.title && (!titleControl.value || !titleControl.touched)) {
                titleControl.setValue(this.scadaSymbolMetadata.title);
              }
            });
          }
        });
      }
    }
  }

  imageFileNameChanged(fileName: string) {
    if (this.uploadImage) {
      const titleControl = this.uploadImageFormGroup.get('title');
      if (!titleControl.value || !titleControl.touched) {
        titleControl.setValue(fileName);
      }
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  upload(): void {
    this.submitted = true;
    let file: File = this.uploadImageFormGroup.get('file').value;
    try {
      if (this.uploadImage) {
        const title: string = this.uploadImageFormGroup.get('title').value;
        if (this.isScada) {
          if (!this.scadaSymbolMetadata) {
            this.scadaSymbolMetadata = emptyMetadata();
          }
          if (this.scadaSymbolMetadata.title !== title) {
            this.scadaSymbolMetadata.title = title;
          }
          const newContent = updateScadaSymbolMetadataInContent(this.scadaSymbolContent, this.scadaSymbolMetadata);
          file = updateFileContent(file, newContent);
        }
        forkJoin([
          this.imageService.uploadImage(file, title, this.data.imageSubType),
          blobToBase64(file)
        ]).subscribe(([imageInfo, base64]) => {
          this.dialogRef.close({image: Object.assign(imageInfo, {base64})});
        });
      } else {
        if (this.isScada) {
          blobToText(file).subscribe(scadaSymbolContent => {
            this.dialogRef.close({scadaSymbolContent});
          });
        } else {
          const image = this.data.image;
          forkJoin([
            this.imageService.updateImage(imageResourceType(image), image.resourceKey, file),
            blobToBase64(file)
          ]).subscribe(([imageInfo, base64]) => {
            this.dialogRef.close({image:Object.assign(imageInfo, {base64})});
          });
        }
      }
    } catch (e) {
      this.store.dispatch(new ActionNotificationShow({
        message: e.message,
        type: 'error',
        verticalPosition: 'top',
        horizontalPosition: 'right',
        target: 'uploadRoot'
      }));
    }
  }
}
