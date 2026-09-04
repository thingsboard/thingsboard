// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { ImageResource, ImageResourceInfo, imageResourceType } from '@shared/models/resource.models';
import {
  UploadImageDialogComponent,
  UploadImageDialogData, UploadImageDialogResult
} from '@shared/components/image/upload-image-dialog.component';
import { UrlHolder } from '@shared/pipe/image.pipe';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EmbedImageDialogComponent, EmbedImageDialogData } from '@shared/components/image/embed-image-dialog.component';

export interface ImageDialogData {
  readonly: boolean;
  image: ImageResourceInfo;
}

@Component({
    selector: 'tb-image-dialog',
    templateUrl: './image-dialog.component.html',
    styleUrls: ['./image-dialog.component.scss'],
    standalone: false
})
export class ImageDialogComponent extends
  DialogComponent<ImageDialogComponent, ImageResourceInfo> implements OnInit {

  image: ImageResourceInfo;

  readonly: boolean;

  imageFormGroup: UntypedFormGroup;

  imageChanged = false;

  imagePreviewData: UrlHolder;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              private dialog: MatDialog,
              private importExportService: ImportExportService,
              @Inject(MAT_DIALOG_DATA) private data: ImageDialogData,
              public dialogRef: MatDialogRef<ImageDialogComponent, ImageResourceInfo>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.image = data.image;
    this.readonly = data.readonly;
    this.imagePreviewData = {
      url: this.image.link
    };
  }

  ngOnInit(): void {
    this.imageFormGroup = this.fb.group({
      title: [this.image.title, [Validators.required]]
    });
    if (this.data.readonly) {
      this.imageFormGroup.disable();
    }
  }

  cancel(): void {
    this.dialogRef.close(this.imageChanged ? this.image : null);
  }

  revertInfo(): void {
    this.imageFormGroup.get('title').setValue(this.image.title);
    this.imageFormGroup.markAsPristine();
  }

  saveInfo(): void {
    const title: string = this.imageFormGroup.get('title').value;
    const image = {...this.image, ...{title}};
    this.imageService.updateImageInfo(image).subscribe(
      (saved) => {
        this.image = saved;
        this.imageChanged = true;
        this.imageFormGroup.markAsPristine();
      }
    );
  }

  downloadImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageService.downloadImage(imageResourceType(this.image), this.image.resourceKey).subscribe();
  }

  exportImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.exportImage(imageResourceType(this.image), this.image.resourceKey);
  }

  embedImage($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EmbedImageDialogComponent, EmbedImageDialogData,
      ImageResourceInfo>(EmbedImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        image: this.image,
        readonly: this.readonly
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.imageChanged = true;
        this.image = result;
        this.imagePreviewData = {
          url: this.image.public ? this.image.publicLink : this.image.link
        };
      }
    });
  }

  updateImage($event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<UploadImageDialogComponent, UploadImageDialogData,
      UploadImageDialogResult>(UploadImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        imageSubType: this.image.resourceSubType,
        image: this.image
      }
    }).afterClosed().subscribe((result) => {
      if (result?.image) {
        this.imageChanged = true;
        this.image = result.image;
        let url;
        if (result.image.base64) {
          url = result.image.base64;
        } else {
          url = this.image.link;
        }
        this.imagePreviewData = {url};
      }
    });
  }

}
