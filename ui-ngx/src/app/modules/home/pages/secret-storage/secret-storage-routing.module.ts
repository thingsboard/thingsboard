// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { SecretStorageTableComponent } from '@home/pages/secret-storage/secret-storage-table.component';

export const secretsRoutes: Routes = [
  {
    path: 'secrets',
    component: SecretStorageTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
      title: 'secret-storage.secrets-storage',
      breadcrumb: {
        menuId: MenuId.secrets
      }
    }
  }
];
