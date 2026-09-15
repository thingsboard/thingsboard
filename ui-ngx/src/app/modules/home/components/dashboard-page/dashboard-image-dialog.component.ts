// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { DashboardService } from '@core/http/dashboard.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import html2canvas from 'html2canvas';
import { map, share } from 'rxjs/operators';
import { BehaviorSubject, from } from 'rxjs';
import { isNumber } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface DashboardImageDialogData {
  dashboardId: DashboardId;
  currentImage?: string;
  dashboardElement: HTMLElement;
}

export interface DashboardImageDialogResult {
  image?: string;
}

@Component({
    selector: 'tb-dashboard-image-dialog',
    templateUrl: './dashboard-image-dialog.component.html',
    styleUrls: ['./dashboard-image-dialog.component.scss'],
    standalone: false
})
export class DashboardImageDialogComponent extends DialogComponent<DashboardImageDialogComponent, DashboardImageDialogResult> {

  takingScreenshotSubject = new BehaviorSubject(false);

  takingScreenshot$ = this.takingScreenshotSubject.asObservable().pipe(
    share()
  );

  dashboardId: DashboardId;
  imageUrl?: string;
  dashboardElement: HTMLElement;

  dashboardRectFormGroup: UntypedFormGroup;
  dashboardImageFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardImageDialogData,
              public dialogRef: MatDialogRef<DashboardImageDialogComponent, DashboardImageDialogResult>,
              private dashboardService: DashboardService,
              private sanitizer: DomSanitizer,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.dashboardId = this.data.dashboardId;
    this.updateImage(this.data.currentImage);
    this.dashboardElement = this.data.dashboardElement;
    const clientRect = this.dashboardElement.getBoundingClientRect();

    this.dashboardRectFormGroup = this.fb.group({
      left: [0, [Validators.min(0), Validators.max(100)]],
      top: [0, [Validators.min(0), Validators.max(100)]],
      right: [100, [Validators.min(0), Validators.max(100)]],
      bottom: [100, [Validators.min(0), Validators.max(100)]]
    });

    this.dashboardImageFormGroup = this.fb.group({
      dashboardImage: [this.data.currentImage]
    });

    this.dashboardImageFormGroup.get('dashboardImage').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
      (newImage) => {
        this.updateImage(newImage);
      }
    );
  }

  private convertUserPercent(percent: any, defaultValue: number): number {
    let result: number;
    if (isNumber(percent)) {
      result = Math.max(0, Math.min(100, percent)) / 100;
    } else {
      result = defaultValue;
    }
    return result;
  }

  takeScreenShot() {
    this.takingScreenshotSubject.next(true);
    const rect = this.dashboardElement.getBoundingClientRect();

    const leftVal = this.convertUserPercent(this.dashboardRectFormGroup.get('left').value, 0);
    const topVal = this.convertUserPercent(this.dashboardRectFormGroup.get('top').value, 0);
    const rightVal = this.convertUserPercent(this.dashboardRectFormGroup.get('right').value, 100);
    const bottomVal = this.convertUserPercent(this.dashboardRectFormGroup.get('bottom').value, 100);

    const left = leftVal * rect.width;
    const top = topVal * rect.height;
    const right = rightVal * rect.width;
    const bottom = bottomVal * rect.height;

    const x = rect.left + left;
    const y = rect.top + top;
    let width = right - left;
    let height = bottom - top;
    width = Math.max(1, width);
    height = Math.max(1, height);
    from(html2canvas(this.dashboardElement, {
      logging: false,
      useCORS: true,
      foreignObjectRendering: false,
      scale: 512 / width,
      x,
      y,
      width,
      height
    })).pipe(
      map(canvas => canvas.toDataURL())).subscribe(
      (image) => {
        this.updateImage(image);
        this.dashboardImageFormGroup.patchValue({dashboardImage: image}, {emitEvent: false});
        this.dashboardImageFormGroup.markAsDirty();
        this.takingScreenshotSubject.next(false);
      },
      (e) => {
        this.takingScreenshotSubject.next(false);
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dashboardService.getDashboard(this.dashboardId.id).subscribe(
      (dashboard) => {
        const newImage: string = this.dashboardImageFormGroup.get('dashboardImage').value;
        dashboard.image = newImage;
        this.dashboardService.saveDashboard(dashboard).subscribe(
          () => {
            this.dialogRef.close({
              image: newImage
            });
          }
        );
      }
    );
  }

  private updateImage(imageUrl: string) {
    this.imageUrl = imageUrl;
  }
}
