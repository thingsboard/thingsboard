// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ImageResourceInfo, ResourceSubType } from '@shared/models/resource.models';

export interface ImageGalleryDialogData {
  imageSubType: ResourceSubType;
}

@Component({
    selector: 'tb-image-gallery-dialog',
    templateUrl: './image-gallery-dialog.component.html',
    styleUrls: ['./image-gallery-dialog.component.scss'],
    standalone: false
})
export class ImageGalleryDialogComponent extends
  DialogComponent<ImageGalleryDialogComponent, ImageResourceInfo> implements OnInit {

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImageGalleryDialogData,
              public dialogRef: MatDialogRef<ImageGalleryDialogComponent, ImageResourceInfo>) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  imageSelected(image: ImageResourceInfo): void {
    this.dialogRef.close(image);
  }

}
