// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { jsonRequired } from '@shared/components/json-object-edit.component';
import {
  CreateProductRequest,
  ProductIssue,
  ProductItem,
  ProductResponse,
  ProductValidationResult,
  ProductIssueDialogData,
  ThingModelView
} from '@shared/models/product.models';
import { ProductService } from '@core/http/product.service';
import { ProductIssuesDialogComponent } from './product-issues-dialog.component';

export type ProductFormMode = 'create' | 'edit';

export interface ProductFormDialogData {
  mode: ProductFormMode;
  product?: ProductItem;
  thingModel?: ThingModelView;
  productId?: string;
}

@Component({
  selector: 'tb-product-form-dialog',
  templateUrl: './product-form-dialog.component.html',
  styleUrls: ['./product-form-dialog.component.scss'],
  standalone: false
})
export class ProductFormDialogComponent
  extends DialogComponent<ProductFormDialogComponent, ProductResponse> {

  entityType = EntityType;

  isSystemAdmin: boolean;

  form: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ProductFormDialogComponent, ProductResponse>,
              @Inject(MAT_DIALOG_DATA) public data: ProductFormDialogData,
              private fb: UntypedFormBuilder,
              private productService: ProductService,
              private dialog: MatDialog,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    const authUser: AuthUser = getCurrentAuthUser(store);
    this.isSystemAdmin = authUser.authority === Authority.SYS_ADMIN;
    const product = data.product;
    const thingModel = data.thingModel;
    this.form = this.fb.group({
      tenantId: [product?.tenantId || null, this.isSystemAdmin ? [Validators.required] : []],
      productId: [{
        value: product?.productId || data.productId || '',
        disabled: data.mode === 'edit'
      }, [Validators.required, Validators.maxLength(64)]],
      name: [product?.name || '', [Validators.required, Validators.maxLength(128)]],
      model: [product?.model || '', [Validators.maxLength(64)]],
      categoryId: [product?.categoryId || '', [Validators.maxLength(64)]],
      tslVersion: [thingModel?.version || product?.tslVersion || '1.0',
        [Validators.required, Validators.maxLength(32)]],
      strictUnknown: [thingModel?.strictUnknown ?? false],
      tsl: [thingModel?.tsl || {}, [jsonRequired]]
    });
  }

  get isCreate(): boolean {
    return this.data.mode === 'create';
  }

  get title(): string {
    return this.isCreate ? 'product.create-product' : 'product.edit-thing-model';
  }

  validateTsl(): void {
    const tsl = this.form.get('tsl').value;
    const productId = this.effectiveProductId();
    if (!productId) {
      this.notify('product.product-id-required', 'error');
      return;
    }
    this.productService.validateThingModel(productId, tsl).subscribe(
      (result: ProductValidationResult) => {
        if (result.valid) {
          this.notify('product.valid', 'success');
        } else {
          this.openIssues('product.validation-failed',
            [...result.errors, ...result.warnings]);
        }
      },
      (error: HttpErrorResponse) => this.handleError(error)
    );
  }

  importJson(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const tsl = JSON.parse(reader.result as string);
        this.form.get('tsl').setValue(tsl);
        this.form.get('tsl').markAsDirty();
        this.form.get('tsl').updateValueAndValidity();
      } catch (e) {
        this.notify('product.invalid-json', 'error');
      }
    };
    reader.readAsText(file);
    input.value = '';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (this.isCreate) {
      const request: CreateProductRequest = {
        productId: value.productId,
        name: value.name,
        model: value.model,
        categoryId: value.categoryId,
        tslVersion: value.tslVersion,
        tsl: value.tsl,
        strictUnknown: value.strictUnknown
      };
      if (this.isSystemAdmin) {
        request.tenantId = this.entityIdValue(value.tenantId);
      }
      this.productService.createProduct(request).subscribe(
        (response) => this.onSaved(response),
        (error: HttpErrorResponse) => this.handleError(error)
      );
    } else {
      this.productService.updateThingModel(this.data.product.productId, {
        tslVersion: value.tslVersion,
        tsl: value.tsl,
        strictUnknown: value.strictUnknown
      }).subscribe(
        (response) => this.onSaved(response),
        (error: HttpErrorResponse) => this.handleError(error)
      );
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private onSaved(response: ProductResponse): void {
    this.notify(this.isCreate ? 'product.create-success' : 'product.save-success', 'success');
    this.dialogRef.close(response);
  }

  private handleError(error: HttpErrorResponse): void {
    const issues: ProductIssue[] = error?.error?.issues;
    if (issues?.length) {
      this.openIssues('product.validation-failed', issues);
    } else {
      this.notify(error?.error?.message || 'product.operation-failed', 'error');
    }
  }

  private openIssues(title: string, issues: ProductIssue[]): void {
    const data: ProductIssueDialogData = {title, issues};
    this.dialog.open(ProductIssuesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data
    });
  }

  private effectiveProductId(): string {
    const productId = this.form.get('productId').value;
    if (productId && productId.trim().length) {
      return productId.trim();
    }
    const tsl = this.form.get('tsl').value;
    return tsl?.profile?.ProductId || tsl?.profile?.productId || tsl?.productId || null;
  }

  private entityIdValue(value: any): string {
    if (!value) {
      return null;
    }
    return typeof value === 'string' ? value : value.id;
  }

  private notify(message: string, type: 'success' | 'error'): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(message),
      type
    }));
  }
}
