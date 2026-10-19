// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { imageSourceType } from '@shared/models/report-component.models';
import {
  extractKeyFromVariable,
  isKeyVariable,
  ReportVariable
} from '@home/pages/reporting/template/components/report-component.models';
import { Observable, of } from 'rxjs';

export interface ReportImageData {
  imageUrl: string;
  width: number;
  height: number;
  entityKeys?: ReportVariable[];
}

@Component({
    selector: 'tb-report-image-dialog',
    templateUrl: './report-image-dialog.component.html',
    styleUrls: ['./report-image-dialog.component.scss'],
    standalone: false
})
export class ReportImageDialogComponent extends DialogComponent<ReportImageDialogComponent, ReportImageData> {

  reportImageFormGroup: UntypedFormGroup;

  lastImageSize: {width: number, height: number};

  origImageSize: {width: number, height: number};

  preserveAspect = true;

  fetchKeyOptionsFn = this.fetchKeyOptions.bind(this);

  private aspect: number = 1;

  private initImageSize = !this.data.width  || !this.data.height || !this.data.imageUrl;

  private entityKeys = this.data.entityKeys;

  private keySearchText: string;
  private latestKeySearchTextResult: Array<string>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ReportImageData,
              public dialogRef: MatDialogRef<ReportImageDialogComponent, ReportImageData>,
              private destroyRef: DestroyRef,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    let sourceType: imageSourceType;
    let imageUrl = '';
    let imageKey = '';

    if (isKeyVariable(data.imageUrl)) {
      sourceType = 'entityKey';
      imageKey = extractKeyFromVariable(data.imageUrl);
    } else {
      sourceType = 'image';
      imageUrl = data.imageUrl;
    }

    this.reportImageFormGroup = this.fb.group({
      sourceType: [sourceType, []],
      imageUrl: [imageUrl, []],
      imageKey: [imageKey, []],
      width: [data.width, [Validators.min(0)]],
      height: [data.height, [Validators.min(0)]]
    });
    if (this.data.width && this.data.height) {
      this.aspect = this.data.width / this.data.height;
    }
    this.reportImageFormGroup.get('width').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.widthUpdated();
    });
    this.reportImageFormGroup.get('height').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.heightUpdated();
    });
    this.reportImageFormGroup.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.sourceTypeUpdated();
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const sourceType: imageSourceType = this.reportImageFormGroup.get('sourceType').value;
    let imageUrl: string;
    if (sourceType === 'image') {
      imageUrl = this.reportImageFormGroup.get('imageUrl').value;
    } else {
      imageUrl = `\${${this.reportImageFormGroup.get('imageKey').value}}`;
    }
    let width: number = this.reportImageFormGroup.get('width').value;
    let height: number = this.reportImageFormGroup.get('height').value;
    if (!width || !height) {
      width = this.origImageSize.width;
      height = this.origImageSize.height;
    }
    const result: ReportImageData = {imageUrl, width, height};
    this.dialogRef.close(result);
  }

  imageSizeUpdated(size: {width: number, height: number}): void {
    this.lastImageSize = size;
    this.updateImageSize(this.lastImageSize);
  }

  private sourceTypeUpdated() {
    const sourceType: imageSourceType = this.reportImageFormGroup.get('sourceType').value;
    if (sourceType === 'image') {
      this.updateImageSize(this.lastImageSize);
    } else {
      this.updateImageSize({width: 200, height: 120});
    }
  }

  private updateImageSize(size: {width: number, height: number}): void {
    this.origImageSize = size;
    if (this.initImageSize) {
      this.aspect = this.origImageSize.width / this.origImageSize.height;
      let width: number = this.reportImageFormGroup.get('width').value;
      if (!width) {
        width = this.origImageSize.width;
      }
      const height = width / this.aspect;
      this.reportImageFormGroup.get('width').patchValue(width, {emitEvent: false});
      this.reportImageFormGroup.get('height').patchValue(height, {emitEvent: false});
    }
    this.initImageSize = true;
  }

  togglePreserveAspect() {
    this.preserveAspect = !this.preserveAspect;
  }

  private widthUpdated() {
    const newWidth = this.reportImageFormGroup.get('width').value;
    if (newWidth) {
      if (this.preserveAspect) {
        const newHeight = newWidth / this.aspect;
        this.reportImageFormGroup.get('height').patchValue(newHeight, {emitEvent: false});
      } else {
        const height = this.reportImageFormGroup.get('height').value;
        if (height) {
          this.aspect = newWidth / height;
        }
      }
    }
  }

  private heightUpdated() {
    const newHeight = this.reportImageFormGroup.get('height').value;
    if (newHeight) {
      if (this.preserveAspect) {
        const newWidth = newHeight * this.aspect;
        this.reportImageFormGroup.get('width').patchValue(newWidth, {emitEvent: false});
      } else {
        const width = this.reportImageFormGroup.get('width').value;
        if (width) {
          this.aspect = width / newHeight;
        }
      }
    }
  }

  private fetchKeyOptions(searchText: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText) {
      this.keySearchText = searchText;
      this.latestKeySearchTextResult = this.entityKeys
                                      .filter(variable => variable.name.toUpperCase().includes(searchText.toUpperCase()))
                                      .map(variable => variable.name);
    }
    return of(this.latestKeySearchTextResult);
  }

}
