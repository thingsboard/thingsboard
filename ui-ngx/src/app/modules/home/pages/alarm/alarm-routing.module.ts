// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { OAuth2Service } from '@core/http/oauth2.service';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AlarmsMode } from '@shared/models/alarm.models';
import { MenuId } from '@core/services/menu.models';

@Injectable()
export class OAuth2LoginProcessingUrlResolver  {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
  }
}

const routes: Routes = [
  {
    path: 'alarms',
    component: AlarmTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'alarm.alarms',
      breadcrumb: {
        menuId: MenuId.alarms
      },
      isPage: true,
      alarmsMode: AlarmsMode.ALL
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class AlarmRoutingModule { }
