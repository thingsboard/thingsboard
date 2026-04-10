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
