// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ProductRoutingModule } from './product-routing.module';
import { ProductsListComponent } from './products-list.component';
import { ProductDetailComponent } from './product-detail.component';
import { ProductFormDialogComponent } from './product-form-dialog.component';
import { ProductIssuesDialogComponent } from './product-issues-dialog.component';

@NgModule({
  declarations: [
    ProductsListComponent,
    ProductDetailComponent,
    ProductFormDialogComponent,
    ProductIssuesDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    ProductRoutingModule
  ]
})
export class ProductModule {
}
