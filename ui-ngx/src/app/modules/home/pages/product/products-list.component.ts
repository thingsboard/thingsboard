// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ProductService } from '@core/http/product.service';
import { Authority } from '@shared/models/authority.enum';
import { PageComponent } from '@shared/components/page.component';
import { ProductItem, ProductStatus } from '@shared/models/product.models';
import { ProductFormDialogComponent } from './product-form-dialog.component';

@Component({
  selector: 'tb-products-list',
  templateUrl: './products-list.component.html',
  styleUrls: ['./products-list.component.scss'],
  standalone: false
})
export class ProductsListComponent extends PageComponent implements OnInit, AfterViewInit {

  displayedColumns: string[] = [];

  dataSource = new MatTableDataSource<ProductItem>([]);

  searchControl = new FormControl('');

  isSystemAdmin = false;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(private productService: ProductService,
              private dialog: MatDialog,
              private router: Router) {
    super();
    this.isSystemAdmin = getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN;
  }

  ngOnInit(): void {
    this.displayedColumns = this.isSystemAdmin
      ? ['productId', 'name', 'model', 'categoryId', 'tslVersion', 'status',
         'propertyCount', 'eventCount', 'actionCount', 'tenantId', 'updatedAt', 'actions']
      : ['productId', 'name', 'model', 'categoryId', 'tslVersion', 'status',
         'propertyCount', 'eventCount', 'actionCount', 'updatedAt', 'actions'];
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => this.loadProducts());
    this.loadProducts();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadProducts(): void {
    this.productService.listProducts(this.searchControl.value).subscribe(
      (products) => {
        this.dataSource.data = products;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
      }
    );
  }

  createProduct(): void {
    this.dialog.open<ProductFormDialogComponent, any, any>(ProductFormDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        mode: 'create'
      }
    }).afterClosed().subscribe((response) => {
      if (response?.productId) {
        this.router.navigate(['/entities/products', response.productId]);
      } else if (response) {
        this.loadProducts();
      }
    });
  }

  openProduct(product: ProductItem, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.router.navigate(['/entities/products', product.productId]);
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
}
