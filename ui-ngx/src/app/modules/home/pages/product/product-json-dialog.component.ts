// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';

export interface ProductJsonDialogData {
  title: string;
  value: any;
}

@Component({
  selector: 'tb-product-json-dialog',
  templateUrl: './product-json-dialog.component.html',
  styleUrls: ['./product-json-dialog.component.scss'],
  standalone: false
})
export class ProductJsonDialogComponent
  extends DialogComponent<ProductJsonDialogComponent, void> {

  title: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ProductJsonDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: ProductJsonDialogData) {
    super(store, router, dialogRef);
    this.title = data.title;
  }

  close(): void {
    this.dialogRef.close();
  }
}
