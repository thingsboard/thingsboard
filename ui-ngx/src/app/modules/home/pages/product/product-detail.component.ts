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
  ProductAction,
  ProductDetail,
  ProductEvent,
  ProductProperty,
  ProductStatus,
  ThingModelView
} from '@shared/models/product.models';
import { ProductFormDialogComponent } from './product-form-dialog.component';
import { ProductJsonDialogComponent, ProductJsonDialogData } from './product-json-dialog.component';

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

  propertyColumns = ['id', 'name', 'dataType', 'mode', 'unit', 'min', 'max', 'required', 'details'];
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

  showPropertyDetails(property: ProductProperty): void {
    this.showJson(property.name || property.id, this.resolvePropertyDefinition(property));
  }

  showEventParams(event: ProductEvent): void {
    this.showJson(`${event.name || event.id} - ${this.translate.instant('product.params')}`,
      event.params);
  }

  showActionInput(action: ProductAction): void {
    this.showJson(`${action.name || action.id} - ${this.translate.instant('product.input')}`,
      action.input);
  }

  showActionOutput(action: ProductAction): void {
    this.showJson(`${action.name || action.id} - ${this.translate.instant('product.output')}`,
      action.output);
  }

  back(): void {
    this.router.navigate(['/entities/products']);
  }

  private showJson(title: string, value: any): void {
    this.dialog.open<ProductJsonDialogComponent, ProductJsonDialogData, void>(ProductJsonDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      width: '80vw',
      maxWidth: '1200px',
      height: '90vh',
      maxHeight: '95vh',
      data: {
        title,
        value
      }
    });
  }

  private resolvePropertyDefinition(property: ProductProperty): any {
    const properties = this.thingModel?.tsl?.properties;
    if (Array.isArray(properties)) {
      return properties.find((item: any) => item?.id === property.id) ?? property;
    }
    return property;
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
