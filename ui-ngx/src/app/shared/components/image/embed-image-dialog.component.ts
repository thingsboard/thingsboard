// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ImageResourceInfo } from '@shared/models/resource.models';
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, UntypedFormBuilder } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface EmbedImageDialogData {
  readonly: boolean;
  image: ImageResourceInfo;
}

@Component({
    selector: 'tb-embed-image-dialog',
    templateUrl: './embed-image-dialog.component.html',
    styleUrls: ['./embed-image-dialog.component.scss'],
    standalone: false
})
export class EmbedImageDialogComponent extends
  DialogComponent<EmbedImageDialogComponent, ImageResourceInfo> implements OnInit {

  image = this.data.image;

  readonly = this.data.readonly;

  imageChanged = false;

  publicStatusControl = new FormControl(this.image.public);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              @Inject(MAT_DIALOG_DATA) private data: EmbedImageDialogData,
              public dialogRef: MatDialogRef<EmbedImageDialogComponent, ImageResourceInfo>,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    if (!this.readonly) {
      this.publicStatusControl.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(
        (isPublic) => {
          this.updateImagePublicStatus(isPublic);
        }
      );
    }
  }

  cancel(): void {
    this.dialogRef.close(this.imageChanged ? this.image : null);
  }

  embedToHtmlCode(): string {
    return '```html\n' +
      '<img src="'+this.image.publicLink+'" alt="'+this.image.title.replace(/"/g, '&quot;')+'" />' +
      '{:copy-code}\n' +
      '```';
  }

  embedToAngularTemplateCode(): string {
    return '```html\n' +
      '<img [src]="\''+this.image.link+'\' | image | async" />' +
      '{:copy-code}\n' +
      '```';
  }

  private updateImagePublicStatus(isPublic: boolean): void {
    this.imageService.updateImagePublicStatus(this.image, isPublic).subscribe(
      (image) => {
        this.image = image;
        this.imageChanged = true;
      }
    );
  }

}
