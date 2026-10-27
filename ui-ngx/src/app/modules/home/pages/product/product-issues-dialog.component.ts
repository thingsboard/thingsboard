// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { ProductIssueDialogData } from '@shared/models/product.models';

@Component({
  selector: 'tb-product-issues-dialog',
  templateUrl: './product-issues-dialog.component.html',
  styleUrls: ['./product-issues-dialog.component.scss'],
  standalone: false
})
export class ProductIssuesDialogComponent
  extends DialogComponent<ProductIssuesDialogComponent, void> {

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ProductIssuesDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: ProductIssueDialogData) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close();
  }
}
