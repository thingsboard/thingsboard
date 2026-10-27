// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ProductService } from '@core/http/product.service';
import { PageComponent } from '@shared/components/page.component';
import {
  ProductDetail,
  ProductStatus,
  ThingModelView
} from '@shared/models/product.models';
import { ProductFormDialogComponent } from './product-form-dialog.component';

@Component({
  selector: 'tb-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  standalone: false
})
export class ProductDetailComponent extends PageComponent implements OnInit {

  productId: string;

  detail: ProductDetail;

  thingModel: ThingModelView;

  propertyColumns = ['id', 'name', 'dataType', 'mode', 'unit', 'min', 'max', 'required'];
  eventColumns = ['id', 'name', 'type', 'params'];
  actionColumns = ['id', 'name', 'input', 'output'];
  versionColumns = ['version', 'status', 'checksum', 'publishedAt', 'createdAt'];

  constructor(private route: ActivatedRoute,
              private router: Router,
              private productService: ProductService,
              private dialog: MatDialog,
              private translate: TranslateService) {
    super();
  }

  ngOnInit(): void {
    this.productId = this.route.snapshot.paramMap.get('productId');
    this.load();
  }

  load(): void {
    this.productService.getProduct(this.productId).subscribe((detail) => {
      this.detail = detail;
      this.productService.getThingModel(this.productId).pipe(
        catchError(() => of(null))
      ).subscribe((thingModel) => {
        this.thingModel = thingModel;
      });
    });
  }

  editThingModel(): void {
    this.dialog.open<ProductFormDialogComponent, any, any>(ProductFormDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        mode: 'edit',
        product: this.detail.product,
        thingModel: this.thingModel,
        productId: this.detail.product.productId
      }
    }).afterClosed().subscribe((response) => {
      if (response) {
        this.notify('product.save-success', 'success');
        this.load();
      }
    });
  }

  back(): void {
    this.router.navigate(['/entities/products']);
  }

  statusTranslationKey(status: ProductStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'product.published';
      case 'DEPRECATED':
        return 'product.deprecated';
      default:
        return 'product.draft';
    }
  }

  statusClass(status: ProductStatus): string {
    return `tb-product-status tb-product-status-${status?.toLowerCase()}`;
  }

  private notify(message: string, type: 'success' | 'error'): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(message),
      type
    }));
  }
}
