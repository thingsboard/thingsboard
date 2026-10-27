// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { ProductsListComponent } from './products-list.component';
import { ProductDetailComponent } from './product-detail.component';

export const productRoutes: Routes = [
  {
    path: 'products',
    data: {
      breadcrumb: {
        menuId: MenuId.products
      }
    },
    children: [
      {
        path: '',
        component: ProductsListComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'product.products'
        }
      },
      {
        path: ':productId',
        component: ProductDetailComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'product.product-details',
          breadcrumb: {
            label: 'product.product-details'
          }
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'products',
    pathMatch: 'full',
    redirectTo: '/entities/products'
  },
  {
    path: 'products/:productId',
    redirectTo: '/entities/products/:productId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProductRoutingModule {
}
