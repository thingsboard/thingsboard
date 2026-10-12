// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { RevokedCertificatesTableComponent } from '@home/pages/device-certificate-revocation/revoked-certificates-table.component';

export const revokedCertificatesRoutes: Routes = [
  {
    path: 'revoked-certificates',
    component: RevokedCertificatesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'device.certificate-revocation.revoked-certificates',
      breadcrumb: {
        menuId: MenuId.revoked_certificates
      }
    }
  }
];
