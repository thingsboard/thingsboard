// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { IFrameViewComponent } from '@home/pages/iframe/iframe-view.component';

const routes: Routes = [
  {
    path: 'iframeView',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      customTitle: true,
      breadcrumb: {
        custom: true
      }
    },
    children: [
      {
        path: '',
        component: IFrameViewComponent
      },
      {
        path: 'child',
        component: IFrameViewComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          customChildTitle: true,
          breadcrumb: {
            customChild: true
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IFrameViewRoutingModule { }
