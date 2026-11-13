// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { TranslationTableComponent } from '@home/pages/custom-translation/translation-table.component';
import { CustomTranslationComponent } from '@home/pages/custom-translation/custom-translation.component';
import { MenuId } from '@core/services/menu.models';

export const CustomTranslationRoutes: Routes = [
  {
    path: 'customTranslation',
    data: {
      breadcrumb: {
        menuId: MenuId.custom_translation
      }
    },
    children: [
      {
        path: '',
        component: TranslationTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-translation.custom-translation',
        }
      },
      {
        path: ':localeCode',
        component: CustomTranslationComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-translation.custom-translation'
        }
      }
    ]
  }
];

@NgModule({})
export class CustomTranslationRoutingModule { }
