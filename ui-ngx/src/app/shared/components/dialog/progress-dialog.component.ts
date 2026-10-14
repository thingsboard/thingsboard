// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface ProgressDialogData<T> {
  progressObservable: Observable<T>;
  progressText: string;
}

@Component({
    selector: 'tb-progress-dialog',
    templateUrl: './progress-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class ProgressDialogComponent<T> {

  progressText: string;

  constructor(public dialogRef: MatDialogRef<ProgressDialogComponent<T>, T>,
              @Inject(MAT_DIALOG_DATA) public data: ProgressDialogData<T>) {
    this.progressText = data.progressText;
    this.data.progressObservable.subscribe(
      (observableData: T) => {
        this.dialogRef.close(observableData);
    },
     () => {
        this.dialogRef.close(null);
    });
  }
}
